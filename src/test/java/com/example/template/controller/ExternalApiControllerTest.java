package com.example.template.controller;

import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.PostResponse;
import com.example.template.service.ExternalApiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalApiControllerTest {

    @Mock
    private ExternalApiService externalApiService;

    @InjectMocks
    private ExternalApiController externalApiController;

    private PostResponse buildPost(Integer id, Integer userId, String title) {
        PostResponse post = new PostResponse();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle(title);
        post.setBody("Body of post " + id);
        return post;
    }

    @Test
    void getAllPosts_returnsOkWithPostList() {
        PostResponse post = buildPost(1, 1, "Title 1");
        when(externalApiService.getAllPosts()).thenReturn(List.of(post));

        ResponseEntity<ApiResponse<List<PostResponse>>> response =
                externalApiController.getAllPosts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).hasSize(1);
        assertThat(response.getBody().getOutputSchema().get(0).getId()).isEqualTo(1);
    }

    @Test
    void getPostById_returnsOkWithPost() {
        PostResponse post = buildPost(1, 1, "Title 1");
        when(externalApiService.getPostById(1L)).thenReturn(post);

        ResponseEntity<ApiResponse<PostResponse>> response =
                externalApiController.getPostById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getTitle()).isEqualTo("Title 1");
    }

    @Test
    void getPostsByUserId_returnsOkWithPostList() {
        PostResponse post = buildPost(1, 2, "Title by User 2");
        when(externalApiService.getPostsByUserId(2)).thenReturn(List.of(post));

        ResponseEntity<ApiResponse<List<PostResponse>>> response =
                externalApiController.getPostsByUserId(2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).hasSize(1);
        assertThat(response.getBody().getOutputSchema().get(0).getUserId()).isEqualTo(2);
    }
}
