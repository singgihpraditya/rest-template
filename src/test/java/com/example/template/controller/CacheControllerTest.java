package com.example.template.controller;

import com.example.template.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheControllerTest {

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private CacheController cacheController;

    @Test
    void cleanup_emptyCacheNames_returnsOk() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<Void>> response = cacheController.cleanup();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).isNull();
        verify(cacheManager, never()).getCache(any());
    }

    @Test
    void cleanup_cacheNotNull_clearsCacheAndReturnsOk() {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(List.of("categoriesWithProductCount"));
        when(cacheManager.getCache("categoriesWithProductCount")).thenReturn(mockCache);

        ResponseEntity<ApiResponse<Void>> response = cacheController.cleanup();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mockCache).clear();
    }

    @Test
    void cleanup_cacheIsNull_skipsAndReturnsOk() {
        when(cacheManager.getCacheNames()).thenReturn(List.of("orphanCache"));
        when(cacheManager.getCache("orphanCache")).thenReturn(null);

        ResponseEntity<ApiResponse<Void>> response = cacheController.cleanup();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // getCache returned null, so no clear() call
    }
}
