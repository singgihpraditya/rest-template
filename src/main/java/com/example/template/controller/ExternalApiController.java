package com.example.template.controller;

import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.PostResponse;
import com.example.template.service.ExternalApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller yang mendemonstrasikan cara memanggil REST API eksternal
 * menggunakan Spring Cloud OpenFeign.
 *
 * API yang digunakan: JSONPlaceholder (https://jsonplaceholder.typicode.com)
 */
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
@Tag(name = "External API", description = "Contoh pemanggilan REST API eksternal via Feign Client")
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    @GetMapping("/posts")
    @Operation(summary = "Get All Posts", description = "Ambil semua posts dari JSONPlaceholder API")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getAllPosts() {
        List<PostResponse> posts = externalApiService.getAllPosts();
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Get Post by ID", description = "Ambil satu post dari JSONPlaceholder API")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long id) {
        PostResponse post = externalApiService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping("/posts/user/{userId}")
    @Operation(summary = "Get Posts by User", description = "Ambil semua posts berdasarkan userId dari JSONPlaceholder")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getPostsByUserId(@PathVariable Integer userId) {
        List<PostResponse> posts = externalApiService.getPostsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
}
