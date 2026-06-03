package com.example.template.controller;

import com.example.template.dto.request.EndpointPermissionRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.EndpointPermissionResponse;
import com.example.template.service.EndpointPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint untuk mengelola aturan otorisasi secara dinamis.
 *
 * Hanya ROLE_ADMIN yang boleh mengakses controller ini.
 * @PreAuthorize di sini berfungsi sebagai SAFETY NET — pengaman berlapis
 * selain rule yang ada di tabel endpoint_permissions itu sendiri.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Permission Management", description = "Kelola aturan otorisasi endpoint secara dinamis")
public class EndpointPermissionController {

    private final EndpointPermissionService permissionService;

    @GetMapping
    @Operation(summary = "List semua rules", description = "Tampilkan semua aturan otorisasi yang aktif")
    public ResponseEntity<ApiResponse<List<EndpointPermissionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail rule", description = "Ambil detail satu aturan berdasarkan ID")
    public ResponseEntity<ApiResponse<EndpointPermissionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Tambah rule baru",
               description = "Buat aturan baru. requiredRole kosong = publik. Gunakan sortOrder lebih kecil untuk rule lebih spesifik.")
    public ResponseEntity<ApiResponse<EndpointPermissionResponse>> create(
            @Valid @RequestBody EndpointPermissionRequest request) {
        EndpointPermissionResponse result = permissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update rule", description = "Update aturan yang sudah ada. Cache otomatis di-refresh.")
    public ResponseEntity<ApiResponse<EndpointPermissionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EndpointPermissionRequest request) {
        EndpointPermissionResponse result = permissionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus rule", description = "Hapus aturan. Cache otomatis di-refresh.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh cache manual",
               description = "Paksa reload semua rules dari DB ke memory. Berguna jika ada perubahan langsung di DB.")
    public ResponseEntity<ApiResponse<Void>> refresh() {
        permissionService.refresh();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
