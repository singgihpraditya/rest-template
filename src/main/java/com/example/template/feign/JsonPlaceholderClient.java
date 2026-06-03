package com.example.template.feign;

import com.example.template.dto.response.PostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign Client untuk memanggil JSONPlaceholder API.
 * JSONPlaceholder adalah free REST API untuk testing: https://jsonplaceholder.typicode.com
 *
 * URL dikonfigurasi di application.properties: external.api.jsonplaceholder.url
 */
@FeignClient(name = "jsonplaceholder", url = "${external.api.jsonplaceholder.url}")
public interface JsonPlaceholderClient {

    /**
     * Ambil semua posts (maksimal 100 data dari JSONPlaceholder).
     */
    @GetMapping("/posts")
    List<PostResponse> getAllPosts();

    /**
     * Ambil satu post berdasarkan ID.
     */
    @GetMapping("/posts/{id}")
    PostResponse getPostById(@PathVariable("id") Long id);

    /**
     * Ambil posts berdasarkan userId.
     */
    @GetMapping("/posts")
    List<PostResponse> getPostsByUserId(@RequestParam("userId") Integer userId);
}
