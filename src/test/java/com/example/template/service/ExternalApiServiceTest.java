package com.example.template.service;

import com.example.template.dto.response.PostResponse;
import com.example.template.feign.JsonPlaceholderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalApiServiceTest {

    @Mock
    private JsonPlaceholderClient jsonPlaceholderClient;

    @InjectMocks
    private ExternalApiService externalApiService;

    private PostResponse buildPost(Integer id, Integer userId, String title, String body) {
        PostResponse post = new PostResponse();
        post.setId(id);
        post.setUserId(userId);
        post.setTitle(title);
        post.setBody(body);
        return post;
    }

    @Test
    void getAllPosts_returnsListFromClient() {
        PostResponse post = buildPost(1, 1, "Title 1", "Body 1");
        when(jsonPlaceholderClient.getAllPosts()).thenReturn(List.of(post));

        List<PostResponse> result = externalApiService.getAllPosts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
        verify(jsonPlaceholderClient).getAllPosts();
    }

    @Test
    void getPostById_returnsPostFromClient() {
        PostResponse post = buildPost(1, 1, "Title 1", "Body 1");
        when(jsonPlaceholderClient.getPostById(1L)).thenReturn(post);

        PostResponse result = externalApiService.getPostById(1L);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getTitle()).isEqualTo("Title 1");
        verify(jsonPlaceholderClient).getPostById(1L);
    }

    @Test
    void getPostsByUserId_returnsListFromClient() {
        PostResponse post = buildPost(1, 2, "Title 2", "Body 2");
        when(jsonPlaceholderClient.getPostsByUserId(2)).thenReturn(List.of(post));

        List<PostResponse> result = externalApiService.getPostsByUserId(2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2);
        verify(jsonPlaceholderClient).getPostsByUserId(2);
    }
}
