package com.example.template.controller;

import com.example.template.dto.request.CategoryRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.CategoryResponse;
import com.example.template.dto.response.PageResponse;
import com.example.template.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Manajemen kategori produk")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List Kategori", description = "Ambil semua kategori dengan pagination dan search opsional")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        PageResponse<CategoryResponse> result = categoryService.findAll(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail Kategori", description = "Ambil detail satu kategori berdasarkan ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse result = categoryService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @Operation(summary = "Tambah Kategori", description = "Buat kategori baru (hanya ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse result = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Kategori", description = "Update data kategori (hanya ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse result = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus Kategori", description = "Hapus kategori (hanya ADMIN, kategori harus kosong)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistik Kategori", description = "Contoh native query: jumlah produk per kategori")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryStats() {
        List<Map<String, Object>> stats = categoryService.getCategoriesWithProductCount();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
