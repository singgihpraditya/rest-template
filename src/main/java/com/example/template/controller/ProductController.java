package com.example.template.controller;

import com.example.template.dto.request.ProductRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.PageResponse;
import com.example.template.dto.response.ProductResponse;
import com.example.template.service.ProductService;
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

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Manajemen produk")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List Produk", description = "Ambil semua produk dengan pagination dan search opsional")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        PageResponse<ProductResponse> result = productService.findAll(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail Produk", description = "Ambil detail satu produk berdasarkan ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse result = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Produk by Kategori", description = "Ambil semua produk dalam satu kategori (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ProductResponse> result = productService.findByCategory(categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/top")
    @Operation(summary = "Top Produk by Harga", description = "Contoh native query: ambil N produk termahal")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopProductsByPrice(
            @RequestParam(defaultValue = "5") int limit) {

        List<ProductResponse> result = productService.getTopProductsByPrice(limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/tag/{tagName}")
    @Operation(summary = "Produk by Tag", description = "Contoh native query: cari produk berdasarkan nama tag")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByTag(
            @PathVariable String tagName) {

        List<ProductResponse> result = productService.findByTag(tagName);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tambah Produk", description = "Buat produk baru (hanya ADMIN)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse result = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Produk", description = "Update data produk (hanya ADMIN)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse result = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hapus Produk", description = "Hapus produk (hanya ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
