package com.example.template.service;

import com.example.template.dto.request.EndpointPermissionRequest;
import com.example.template.dto.response.EndpointPermissionResponse;
import com.example.template.entity.EndpointPermission;
import com.example.template.exception.ResourceNotFoundException;
import com.example.template.repository.EndpointPermissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service untuk mengelola aturan otorisasi dinamis.
 *
 * CARA KERJA CACHE:
 * - Saat startup (@PostConstruct), semua rule aktif dimuat ke memori.
 * - Setiap perubahan (create/update/delete) otomatis memanggil refresh().
 * - refresh() bisa juga dipanggil manual via endpoint /api/permissions/refresh.
 * - Field permissionCache bersifat volatile → penggantian reference-nya thread-safe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointPermissionService {

    private final EndpointPermissionRepository repository;

    // Ant-style URL matcher: /api/products/** cocok dengan /api/products/1, /api/products/1/tags, dll.
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Cache in-memory. volatile: penggantian reference saat refresh() thread-safe.
    private volatile List<EndpointPermission> permissionCache = Collections.emptyList();

    @PostConstruct
    public void loadPermissions() {
        refresh();
    }

    /**
     * Reload semua rule aktif dari DB ke cache.
     * Dipanggil otomatis setelah setiap perubahan data, atau manual via /api/permissions/refresh.
     */
    public void refresh() {
        List<EndpointPermission> loaded = repository.findAllByActiveTrueOrderBySortOrderAsc();
        permissionCache = loaded;
        log.info("Permission cache refreshed: {} active rules loaded", loaded.size());
    }

    /**
     * Cari rule pertama yang cocok dengan method dan path.
     * Rule dengan sortOrder lebih kecil dicek lebih dulu (lebih prioritas).
     *
     * @return rule yang cocok, atau empty jika tidak ada
     */
    @Transactional(readOnly = true)
    public Optional<EndpointPermission> findMatchingPermission(String method, String path) {
        List<EndpointPermission> perms = permissionCache;

        // Lazy load: jika cache kosong (misal saat startup sebelum DataInitializer selesai)
        if (perms.isEmpty()) {
            refresh();
            perms = permissionCache;
        }

        return perms.stream()
                .filter(p -> matchesMethod(p.getHttpMethod(), method))
                .filter(p -> pathMatcher.match(p.getUrlPattern(), path))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<EndpointPermissionResponse> findAll() {
        return repository.findAllByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(EndpointPermissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EndpointPermissionResponse findById(Long id) {
        return repository.findById(id)
                .map(EndpointPermissionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("EndpointPermission", id));
    }

    @Transactional
    public EndpointPermissionResponse create(EndpointPermissionRequest request) {
        EndpointPermission permission = EndpointPermission.builder()
                .httpMethod(request.getHttpMethod().toUpperCase())
                .urlPattern(request.getUrlPattern())
                .requiredRole(request.getRequiredRole())
                .sortOrder(request.getSortOrder())
                .active(request.isActive())
                .description(request.getDescription())
                .build();

        EndpointPermission saved = repository.save(permission);
        refresh(); // Update cache
        log.info("Permission rule dibuat: [{} {}] -> {}", saved.getHttpMethod(), saved.getUrlPattern(), saved.getRequiredRole());
        return EndpointPermissionResponse.from(saved);
    }

    @Transactional
    public EndpointPermissionResponse update(Long id, EndpointPermissionRequest request) {
        EndpointPermission existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EndpointPermission", id));

        existing.setHttpMethod(request.getHttpMethod().toUpperCase());
        existing.setUrlPattern(request.getUrlPattern());
        existing.setRequiredRole(request.getRequiredRole());
        existing.setSortOrder(request.getSortOrder());
        existing.setActive(request.isActive());
        existing.setDescription(request.getDescription());

        EndpointPermission saved = repository.save(existing);
        refresh(); // Update cache
        log.info("Permission rule diupdate: [{} {}] -> {}", saved.getHttpMethod(), saved.getUrlPattern(), saved.getRequiredRole());
        return EndpointPermissionResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        EndpointPermission permission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EndpointPermission", id));
        repository.delete(permission);
        refresh(); // Update cache
        log.info("Permission rule dihapus: [{} {}]", permission.getHttpMethod(), permission.getUrlPattern());
    }

    // "*" di httpMethod artinya cocok dengan semua HTTP method
    private boolean matchesMethod(String ruleMethod, String requestMethod) {
        return "*".equals(ruleMethod) || ruleMethod.equalsIgnoreCase(requestMethod);
    }
}
