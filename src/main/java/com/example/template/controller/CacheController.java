package com.example.template.controller;

import com.example.template.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint untuk membersihkan (reset) seluruh Spring Cache secara manual.
 *
 * Hanya ROLE_ADMIN yang boleh mengakses controller ini.
 * @PreAuthorize di sini berfungsi sebagai SAFETY NET — pengaman berlapis
 * selain rule yang ada di tabel endpoint_permissions itu sendiri.
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cache Management", description = "Kelola Spring Cache secara manual")
public class CacheController {

    private final CacheManager cacheManager;

    @PostMapping("/cleanup")
    @Operation(summary = "Bersihkan semua cache",
               description = "Hapus seluruh isi cache (semua cache name) sehingga data berikutnya diambil ulang dari DB.")
    public ResponseEntity<ApiResponse<Void>> cleanup() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
