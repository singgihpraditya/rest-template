package com.example.template.service;

import com.example.template.dto.request.EndpointPermissionRequest;
import com.example.template.dto.response.EndpointPermissionResponse;
import com.example.template.entity.EndpointPermission;
import com.example.template.exception.ResourceNotFoundException;
import com.example.template.repository.EndpointPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointPermissionServiceTest {

    @Mock
    private EndpointPermissionRepository repository;

    @InjectMocks
    private EndpointPermissionService permissionService;

    private EndpointPermission buildPermission(Long id, String method, String pattern, String role, int order) {
        return EndpointPermission.builder()
                .id(id).httpMethod(method).urlPattern(pattern)
                .requiredRole(role).sortOrder(order).active(true)
                .description("Test permission").build();
    }

    private EndpointPermissionRequest buildRequest(String method, String pattern, String role, int order) {
        EndpointPermissionRequest req = new EndpointPermissionRequest();
        ReflectionTestUtils.setField(req, "httpMethod", method);
        ReflectionTestUtils.setField(req, "urlPattern", pattern);
        ReflectionTestUtils.setField(req, "requiredRole", role);
        ReflectionTestUtils.setField(req, "sortOrder", order);
        ReflectionTestUtils.setField(req, "active", true);
        ReflectionTestUtils.setField(req, "description", "Test");
        return req;
    }

    @Test
    void loadPermissions_callsRefresh() {
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(Collections.emptyList());

        permissionService.loadPermissions();

        verify(repository).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void refresh_loadsPermissionsFromRepositoryAndUpdatesCache() {
        EndpointPermission perm = buildPermission(1L, "GET", "/api/**", null, 10);
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(perm));

        permissionService.refresh();

        @SuppressWarnings("unchecked")
        List<EndpointPermission> cache =
                (List<EndpointPermission>) ReflectionTestUtils.getField(permissionService, "permissionCache");
        assertThat(cache).hasSize(1).contains(perm);
        verify(repository).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void findMatchingPermission_cachePopulated_matchFound() {
        EndpointPermission perm = buildPermission(1L, "GET", "/api/products/**", "ROLE_USER", 10);
        ReflectionTestUtils.setField(permissionService, "permissionCache", List.of(perm));

        Optional<EndpointPermission> result =
                permissionService.findMatchingPermission("GET", "/api/products/1");

        assertThat(result).isPresent();
        assertThat(result.get().getRequiredRole()).isEqualTo("ROLE_USER");
        verify(repository, never()).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void findMatchingPermission_cachePopulated_noMatch() {
        EndpointPermission perm = buildPermission(1L, "POST", "/api/admin/**", "ROLE_ADMIN", 10);
        ReflectionTestUtils.setField(permissionService, "permissionCache", List.of(perm));

        Optional<EndpointPermission> result =
                permissionService.findMatchingPermission("GET", "/api/products");

        assertThat(result).isEmpty();
    }

    @Test
    void findMatchingPermission_cacheEmpty_lazyLoadsThenMatchFound() {
        // cache stays empty (no @PostConstruct called by Mockito)
        EndpointPermission perm = buildPermission(1L, "GET", "/api/products/**", null, 10);
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(perm));

        Optional<EndpointPermission> result =
                permissionService.findMatchingPermission("GET", "/api/products/1");

        assertThat(result).isPresent();
        verify(repository).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void findMatchingPermission_wildcardMethod_matchesAnyMethod() {
        EndpointPermission perm = buildPermission(1L, "*", "/api/auth/**", null, 2);
        ReflectionTestUtils.setField(permissionService, "permissionCache", List.of(perm));

        assertThat(permissionService.findMatchingPermission("POST", "/api/auth/login")).isPresent();
        assertThat(permissionService.findMatchingPermission("GET", "/api/auth/me")).isPresent();
        assertThat(permissionService.findMatchingPermission("DELETE", "/api/auth/logout")).isPresent();
    }

    @Test
    void findMatchingPermission_caseInsensitiveMethod_matchFound() {
        EndpointPermission perm = buildPermission(1L, "GET", "/api/categories/**", null, 11);
        ReflectionTestUtils.setField(permissionService, "permissionCache", List.of(perm));

        Optional<EndpointPermission> result =
                permissionService.findMatchingPermission("get", "/api/categories/1");

        assertThat(result).isPresent();
    }

    @Test
    void findAll_returnsActivePermissions() {
        EndpointPermission perm = buildPermission(1L, "GET", "/api/**", null, 10);
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(perm));

        List<EndpointPermissionResponse> result = permissionService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHttpMethod()).isEqualTo("GET");
        assertThat(result.get(0).getUrlPattern()).isEqualTo("/api/**");
    }

    @Test
    void findById_success() {
        EndpointPermission perm = buildPermission(1L, "GET", "/api/**", null, 10);
        when(repository.findById(1L)).thenReturn(Optional.of(perm));

        EndpointPermissionResponse result = permissionService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_success_andRefreshesCache() {
        EndpointPermissionRequest request = buildRequest("GET", "/api/orders/**", "ROLE_USER", 50);
        EndpointPermission saved = buildPermission(1L, "GET", "/api/orders/**", "ROLE_USER", 50);
        when(repository.save(any(EndpointPermission.class))).thenReturn(saved);
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(saved));

        EndpointPermissionResponse result = permissionService.create(request);

        assertThat(result.getHttpMethod()).isEqualTo("GET");
        assertThat(result.getUrlPattern()).isEqualTo("/api/orders/**");
        verify(repository).save(any(EndpointPermission.class));
        // refresh() is called after save
        verify(repository).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void create_httpMethodUppercased() {
        EndpointPermissionRequest request = buildRequest("post", "/api/test/**", null, 100);
        EndpointPermission saved = buildPermission(1L, "POST", "/api/test/**", null, 100);
        when(repository.save(any(EndpointPermission.class))).thenReturn(saved);
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(Collections.emptyList());

        permissionService.create(request);

        verify(repository).save(argThat(p -> "POST".equals(p.getHttpMethod())));
    }

    @Test
    void update_success_andRefreshesCache() {
        EndpointPermission existing = buildPermission(1L, "GET", "/api/old/**", null, 10);
        EndpointPermissionRequest request = buildRequest("POST", "/api/new/**", "ROLE_ADMIN", 20);
        EndpointPermission saved = buildPermission(1L, "POST", "/api/new/**", "ROLE_ADMIN", 20);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(EndpointPermission.class))).thenReturn(saved);
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(saved));

        EndpointPermissionResponse result = permissionService.update(1L, request);

        assertThat(result.getHttpMethod()).isEqualTo("POST");
        verify(repository).save(any(EndpointPermission.class));
        verify(repository).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        EndpointPermissionRequest request = buildRequest("GET", "/api/**", null, 10);
        assertThatThrownBy(() -> permissionService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_success_andRefreshesCache() {
        EndpointPermission perm = buildPermission(1L, "GET", "/api/**", null, 10);
        when(repository.findById(1L)).thenReturn(Optional.of(perm));
        when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(Collections.emptyList());

        permissionService.delete(1L);

        verify(repository).delete(perm);
        verify(repository).findAllByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
