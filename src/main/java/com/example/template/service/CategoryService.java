package com.example.template.service;

import com.example.template.dto.request.CategoryRequest;
import com.example.template.dto.response.CategoryResponse;
import com.example.template.dto.response.PageResponse;
import com.example.template.entity.Category;
import com.example.template.exception.BusinessException;
import com.example.template.exception.ResourceNotFoundException;
import com.example.template.repository.CategoryRepository;
import com.example.template.repository.projection.CategoryProductCountProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Kategori dengan nama '" + request.getName() + "' sudah ada");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Kategori baru dibuat: {}", saved.getName());
        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> findAll(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Page<Category> categoryPage;
        if (StringUtils.hasText(search)) {
            categoryPage = categoryRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }

        Page<CategoryResponse> responsePage = categoryPage.map(CategoryResponse::from);
        return PageResponse.of(responsePage);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Cek apakah nama baru sudah dipakai kategori lain
        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Kategori dengan nama '" + request.getName() + "' sudah ada");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category updated = categoryRepository.save(category);
        log.info("Kategori diupdate: {}", updated.getName());
        return CategoryResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!category.getProducts().isEmpty()) {
            throw new BusinessException("Kategori tidak bisa dihapus karena masih memiliki produk");
        }

        categoryRepository.delete(category);
        log.info("Kategori dihapus: {}", category.getName());
    }

    /**
     * Contoh penggunaan native query:
     * Mendapatkan kategori beserta jumlah produk masing-masing.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoriesWithProductCount() {
        List<CategoryProductCountProjection> results = categoryRepository.findCategoriesWithProductCount();

        return results.stream().map(row -> Map.of(
                "id", row.getId(),
                "name", row.getName(),
                "description", row.getDescription() != null ? row.getDescription() : "",
                "product_count", row.getProductCount()
        )).toList();
    }
}
