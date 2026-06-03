package com.example.template.service;

import com.example.template.dto.response.PostResponse;
import com.example.template.feign.JsonPlaceholderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service untuk memanggil API eksternal menggunakan Feign Client.
 * Contoh: memanggil JSONPlaceholder (https://jsonplaceholder.typicode.com)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiService {

    private final JsonPlaceholderClient jsonPlaceholderClient;

    public List<PostResponse> getAllPosts() {
        log.info("Memanggil external API: GET /posts");
        return jsonPlaceholderClient.getAllPosts();
    }

    public PostResponse getPostById(Long id) {
        log.info("Memanggil external API: GET /posts/{}", id);
        return jsonPlaceholderClient.getPostById(id);
    }

    public List<PostResponse> getPostsByUserId(Integer userId) {
        log.info("Memanggil external API: GET /posts?userId={}", userId);
        return jsonPlaceholderClient.getPostsByUserId(userId);
    }
}
