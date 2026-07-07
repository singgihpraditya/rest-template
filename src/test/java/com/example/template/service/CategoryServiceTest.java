package com.example.template.service;

import com.example.template.dto.request.CategoryRequest;
import com.example.template.dto.response.CategoryResponse;
import com.example.template.dto.response.PageResponse;
import com.example.template.entity.Category;
import com.example.template.exception.BusinessException;
import com.example.template.exception.ResourceNotFoundException;
import com.example.template.repository.CategoryRepository;
import com.example.template.repository.projection.CategoryProductCountProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryRequest buildRequest(String name, String description) {
        CategoryRequest req = new CategoryRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "description", description);
        return req;
    }

    private Category buildCategory(Long id, String name) {
        return Category.builder().id(id).name(name).description("Desc " + name).build();
    }

    @Test
    void create_success() {
        CategoryRequest request = buildRequest("Electronics", "Perangkat elektronik");
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);

        Category saved = buildCategory(1L, "Electronics");
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse result = categoryService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_nameDuplicate_throwsBusinessException() {
        CategoryRequest request = buildRequest("Electronics", "Desc");
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Electronics");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void findById_success() {
        Category category = buildCategory(1L, "Books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Books");
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_withoutSearch_returnsPaginatedResult() {
        Category category = buildCategory(1L, "Electronics");
        Page<Category> page = new PageImpl<>(List.of(category));
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<CategoryResponse> result = categoryService.findAll(0, 10, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(categoryRepository).findAll(any(Pageable.class));
        verify(categoryRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findAll_withSearch_returnsFilteredResult() {
        Category category = buildCategory(1L, "Electronics");
        Page<Category> page = new PageImpl<>(List.of(category));
        when(categoryRepository.findByNameContainingIgnoreCase(eq("elec"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<CategoryResponse> result = categoryService.findAll(0, 10, "elec");

        assertThat(result.getContent()).hasSize(1);
        verify(categoryRepository).findByNameContainingIgnoreCase(eq("elec"), any(Pageable.class));
        verify(categoryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void update_sameName_success() {
        Category existing = buildCategory(1L, "Electronics");
        CategoryRequest request = buildRequest("Electronics", "Updated description");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        CategoryResponse result = categoryService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository, never()).existsByName(any());
    }

    @Test
    void update_differentNameNotDuplicate_success() {
        Category existing = buildCategory(1L, "Electronics");
        CategoryRequest request = buildRequest("Gadgets", "New description");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Gadgets")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        CategoryResponse result = categoryService.update(1L, request);

        assertThat(result).isNotNull();
        verify(categoryRepository).existsByName("Gadgets");
    }

    @Test
    void update_differentNameDuplicate_throwsBusinessException() {
        Category existing = buildCategory(1L, "Electronics");
        CategoryRequest request = buildRequest("Books", "Desc");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Books")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Books");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        CategoryRequest request = buildRequest("Electronics", "Desc");
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_success() {
        Category category = buildCategory(1L, "Empty Category");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_hasProducts_throwsBusinessException() {
        Category category = buildCategory(1L, "Electronics");
        // Add a product to the list to simulate non-empty category
        category.getProducts().add(
            com.example.template.entity.Product.builder()
                .id(1L).name("iPhone").price(java.math.BigDecimal.TEN).stock(5)
                .category(category).build()
        );
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("masih memiliki produk");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCategoriesWithProductCount_withDescription_returnsMappedList() {
        CategoryProductCountProjection projection = mock(CategoryProductCountProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getName()).thenReturn("Electronics");
        when(projection.getDescription()).thenReturn("Perangkat elektronik");
        when(projection.getProductCount()).thenReturn(5L);
        when(categoryRepository.findCategoriesWithProductCount()).thenReturn(List.of(projection));

        List<Map<String, Object>> result = categoryService.getCategoriesWithProductCount();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("id")).isEqualTo(1L);
        assertThat(result.get(0).get("name")).isEqualTo("Electronics");
        assertThat(result.get(0).get("description")).isEqualTo("Perangkat elektronik");
        assertThat(result.get(0).get("product_count")).isEqualTo(5L);
    }

    @Test
    void getCategoriesWithProductCount_nullDescription_returnsEmptyString() {
        CategoryProductCountProjection projection = mock(CategoryProductCountProjection.class);
        when(projection.getId()).thenReturn(2L);
        when(projection.getName()).thenReturn("Books");
        when(projection.getDescription()).thenReturn(null);
        when(projection.getProductCount()).thenReturn(3L);
        when(categoryRepository.findCategoriesWithProductCount()).thenReturn(List.of(projection));

        List<Map<String, Object>> result = categoryService.getCategoriesWithProductCount();

        assertThat(result.get(0).get("description")).isEqualTo("");
    }

    @Test
    void getCategoriesWithProductCount_emptyList_returnsEmptyList() {
        when(categoryRepository.findCategoriesWithProductCount()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = categoryService.getCategoriesWithProductCount();

        assertThat(result).isEmpty();
    }
}
