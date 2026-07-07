package com.example.template.controller;

import com.example.template.dto.request.ProductRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.CategoryResponse;
import com.example.template.dto.response.PageResponse;
import com.example.template.dto.response.ProductResponse;
import com.example.template.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductResponse buildProductResponse(Long id, String name) {
        CategoryResponse category = CategoryResponse.builder()
                .id(1L).name("Electronics").build();
        return ProductResponse.builder()
                .id(id).name(name).description("Desc")
                .price(new BigDecimal("999.99")).stock(10)
                .category(category).tags(new HashSet<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private PageResponse<ProductResponse> buildPage(ProductResponse response) {
        return PageResponse.<ProductResponse>builder()
                .content(List.of(response)).pageNumber(0).pageSize(10)
                .totalElements(1).totalPages(1).first(true).last(true).build();
    }

    private ProductRequest buildRequest(String name, Long categoryId) {
        ProductRequest req = new ProductRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "description", "Test");
        ReflectionTestUtils.setField(req, "price", new BigDecimal("999.99"));
        ReflectionTestUtils.setField(req, "stock", 10);
        ReflectionTestUtils.setField(req, "categoryId", categoryId);
        ReflectionTestUtils.setField(req, "tagIds", new HashSet<>());
        return req;
    }

    @Test
    void getAllProducts_returnsOkWithPageResponse() {
        ProductResponse product = buildProductResponse(1L, "iPhone 15");
        when(productService.findAll(0, 10, null)).thenReturn(buildPage(product));

        ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> response =
                productController.getAllProducts(0, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getContent()).hasSize(1);
    }

    @Test
    void getAllProducts_withSearch_passesSearchToService() {
        when(productService.findAll(0, 10, "iphone")).thenReturn(
                PageResponse.<ProductResponse>builder().content(List.of())
                        .pageNumber(0).pageSize(10).totalElements(0).totalPages(0)
                        .first(true).last(true).build());

        productController.getAllProducts(0, 10, "iphone");

        verify(productService).findAll(0, 10, "iphone");
    }

    @Test
    void getProductById_returnsOkWithProduct() {
        ProductResponse product = buildProductResponse(1L, "iPhone 15");
        when(productService.findById(1L)).thenReturn(product);

        ResponseEntity<ApiResponse<ProductResponse>> response =
                productController.getProductById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getId()).isEqualTo(1L);
    }

    @Test
    void getProductsByCategory_returnsOkWithPageResponse() {
        ProductResponse product = buildProductResponse(1L, "iPhone 15");
        when(productService.findByCategory(1L, 0, 10)).thenReturn(buildPage(product));

        ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> response =
                productController.getProductsByCategory(1L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getContent()).hasSize(1);
    }

    @Test
    void getTopProductsByPrice_returnsOkWithList() {
        ProductResponse product = buildProductResponse(1L, "MacBook Pro");
        when(productService.getTopProductsByPrice(5)).thenReturn(List.of(product));

        ResponseEntity<ApiResponse<List<ProductResponse>>> response =
                productController.getTopProductsByPrice(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).hasSize(1);
    }

    @Test
    void getProductsByTag_returnsOkWithList() {
        ProductResponse product = buildProductResponse(1L, "iPhone 15");
        when(productService.findByTag("smartphone")).thenReturn(List.of(product));

        ResponseEntity<ApiResponse<List<ProductResponse>>> response =
                productController.getProductsByTag("smartphone");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).hasSize(1);
    }

    @Test
    void createProduct_returnsCreatedWithProductResponse() {
        ProductRequest request = buildRequest("iPhone 15", 1L);
        ProductResponse product = buildProductResponse(1L, "iPhone 15");
        when(productService.create(request)).thenReturn(product);

        ResponseEntity<ApiResponse<ProductResponse>> response =
                productController.createProduct(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOutputSchema().getName()).isEqualTo("iPhone 15");
    }

    @Test
    void updateProduct_returnsOkWithUpdatedProduct() {
        ProductRequest request = buildRequest("iPhone 15 Pro", 1L);
        ProductResponse product = buildProductResponse(1L, "iPhone 15 Pro");
        when(productService.update(1L, request)).thenReturn(product);

        ResponseEntity<ApiResponse<ProductResponse>> response =
                productController.updateProduct(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema().getName()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    void deleteProduct_returnsOkWithNullData() {
        ResponseEntity<ApiResponse<Void>> response =
                productController.deleteProduct(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOutputSchema()).isNull();
        verify(productService).delete(1L);
    }
}
