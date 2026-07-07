package com.example.template.service;

import com.example.template.dto.request.ProductRequest;
import com.example.template.dto.response.PageResponse;
import com.example.template.dto.response.ProductResponse;
import com.example.template.entity.Category;
import com.example.template.entity.Product;
import com.example.template.entity.Tag;
import com.example.template.exception.ResourceNotFoundException;
import com.example.template.repository.CategoryRepository;
import com.example.template.repository.ProductRepository;
import com.example.template.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;

    @InjectMocks
    private ProductService productService;

    private Category buildCategory(Long id, String name) {
        return Category.builder().id(id).name(name).description("Desc").build();
    }

    private Tag buildTag(Long id, String name) {
        return Tag.builder().id(id).name(name).build();
    }

    private Product buildProduct(Long id, String name, Category category) {
        return Product.builder()
                .id(id).name(name).description("Desc")
                .price(new BigDecimal("999.99")).stock(10)
                .imageUrl("http://img.com/test.jpg")
                .publishedAt(LocalDateTime.now())
                .category(category).build();
    }

    private ProductRequest buildRequest(String name, Long categoryId, Set<Long> tagIds) {
        ProductRequest req = new ProductRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "description", "Test description");
        ReflectionTestUtils.setField(req, "price", new BigDecimal("999.99"));
        ReflectionTestUtils.setField(req, "stock", 10);
        ReflectionTestUtils.setField(req, "imageUrl", "http://img.com/test.jpg");
        ReflectionTestUtils.setField(req, "categoryId", categoryId);
        ReflectionTestUtils.setField(req, "tagIds", tagIds != null ? tagIds : new HashSet<>());
        ReflectionTestUtils.setField(req, "publishedAt", LocalDateTime.now());
        return req;
    }

    @Test
    void create_success() {
        Category category = buildCategory(1L, "Electronics");
        Tag tag = buildTag(1L, "smartphone");
        Product saved = buildProduct(1L, "iPhone 15", category);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(tagRepository.findByIdIn(Set.of(1L))).thenReturn(Set.of(tag));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductRequest request = buildRequest("iPhone 15", 1L, Set.of(1L));
        ProductResponse result = productService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("iPhone 15");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_categoryNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = buildRequest("iPhone 15", 99L, new HashSet<>());
        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void findById_success() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "MacBook Pro", category);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("MacBook Pro");
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_withoutSearch_returnsPaginatedResult() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "iPhone 15", category);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> result = productService.findAll(0, 10, null);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findAll(any(Pageable.class));
        verify(productRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findAll_withSearch_returnsFilteredResult() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "iPhone 15", category);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByNameContainingIgnoreCase(eq("iphone"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.findAll(0, 10, "iphone");

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByNameContainingIgnoreCase(eq("iphone"), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findByCategory_success() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "iPhone 15", category);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.findByCategoryId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> result = productService.findByCategory(1L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByCategoryId(eq(1L), any(Pageable.class));
    }

    @Test
    void findByCategory_categoryNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByCategory(99L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).findByCategoryId(any(), any());
    }

    @Test
    void update_success() {
        Category oldCategory = buildCategory(1L, "Electronics");
        Category newCategory = buildCategory(2L, "Mobile");
        Product existing = buildProduct(1L, "iPhone 14", oldCategory);
        Tag tag = buildTag(1L, "smartphone");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(tagRepository.findByIdIn(Set.of(1L))).thenReturn(Set.of(tag));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        ProductRequest request = buildRequest("iPhone 15", 2L, Set.of(1L));
        ProductResponse result = productService.update(1L, request);

        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_productNotFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = buildRequest("iPhone 15", 1L, new HashSet<>());
        assertThatThrownBy(() -> productService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_categoryNotFound_throwsResourceNotFoundException() {
        Category oldCategory = buildCategory(1L, "Electronics");
        Product existing = buildProduct(1L, "iPhone 14", oldCategory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = buildRequest("iPhone 15", 99L, new HashSet<>());
        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_success() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "iPhone 15", category);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void getTopProductsByPrice_success() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "MacBook Pro", category);
        when(productRepository.findTopByPriceNative(5)).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getTopProductsByPrice(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("MacBook Pro");
    }

    @Test
    void findByTag_success() {
        Category category = buildCategory(1L, "Electronics");
        Product product = buildProduct(1L, "iPhone 15", category);
        when(productRepository.findByTagName("smartphone")).thenReturn(List.of(product));

        List<ProductResponse> result = productService.findByTag("smartphone");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("iPhone 15");
    }
}
