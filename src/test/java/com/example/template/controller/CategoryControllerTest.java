package com.example.template.controller;

import com.example.template.dto.request.CategoryRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.CategoryResponse;
import com.example.template.dto.response.PageResponse;
import com.example.template.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryResponse buildCategoryResponse(Long id, String name) {
        return CategoryResponse.builder()
                .id(id).name(name).description("Desc " + name)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private CategoryRequest buildRequest(String name, String description) {
        CategoryRequest req = new CategoryRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "description", description);
        return req;
    }

    @Test
    void getAllCategories_returnsOkWithPageResponse() {
        CategoryResponse catResponse = buildCategoryResponse(1L, "Electronics");
        PageResponse<CategoryResponse> pageResponse = PageResponse.<CategoryResponse>builder()
                .content(List.of(catResponse)).pageNumber(0).pageSize(10)
                .totalElements(1).totalPages(1).first(true).last(true).build();
        when(categoryService.findAll(0, 10, null)).thenReturn(pageResponse);

        ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> response =
                categoryController.getAllCategories(0, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getContent()).hasSize(1);
    }

    @Test
    void getAllCategories_withSearch_passesSearchToService() {
        PageResponse<CategoryResponse> pageResponse = PageResponse.<CategoryResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(10)
                .totalElements(0).totalPages(0).first(true).last(true).build();
        when(categoryService.findAll(0, 10, "elec")).thenReturn(pageResponse);

        ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> response =
                categoryController.getAllCategories(0, 10, "elec");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(categoryService).findAll(0, 10, "elec");
    }

    @Test
    void getCategoryById_returnsOkWithCategory() {
        CategoryResponse catResponse = buildCategoryResponse(1L, "Electronics");
        when(categoryService.findById(1L)).thenReturn(catResponse);

        ResponseEntity<ApiResponse<CategoryResponse>> response =
                categoryController.getCategoryById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getId()).isEqualTo(1L);
    }

    @Test
    void createCategory_returnsCreatedWithCategoryResponse() {
        CategoryRequest request = buildRequest("Electronics", "Perangkat elektronik");
        CategoryResponse catResponse = buildCategoryResponse(1L, "Electronics");
        when(categoryService.create(request)).thenReturn(catResponse);

        ResponseEntity<ApiResponse<CategoryResponse>> response =
                categoryController.createCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOutputSchema().getName()).isEqualTo("Electronics");
    }

    @Test
    void updateCategory_returnsOkWithUpdatedCategory() {
        CategoryRequest request = buildRequest("Electronics Updated", "New desc");
        CategoryResponse catResponse = buildCategoryResponse(1L, "Electronics Updated");
        when(categoryService.update(1L, request)).thenReturn(catResponse);

        ResponseEntity<ApiResponse<CategoryResponse>> response =
                categoryController.updateCategory(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getName()).isEqualTo("Electronics Updated");
    }

    @Test
    void deleteCategory_returnsOkWithNullData() {
        ResponseEntity<ApiResponse<Void>> response =
                categoryController.deleteCategory(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).isNull();
        verify(categoryService).delete(1L);
    }

    @Test
    void getCategoryStats_returnsOkWithStatsData() {
        List<Map<String, Object>> stats = List.of(
                Map.of("id", 1L, "name", "Electronics", "product_count", 5L));
        when(categoryService.getCategoriesWithProductCount()).thenReturn(stats);

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                categoryController.getCategoryStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).hasSize(1);
    }
}
