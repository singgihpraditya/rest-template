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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .publishedAt(request.getPublishedAt())
                .category(category)
                .tags(tags)
                .build();

        Product saved = productRepository.save(product);
        log.info("Produk baru dibuat: {}", saved.getName());
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Product> productPage;
        if (StringUtils.hasText(search)) {
            productPage = productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        Page<ProductResponse> responsePage = productPage.map(ProductResponse::from);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findByCategory(Long categoryId, int page, int size) {
        // Pastikan kategori ada
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findByCategoryId(categoryId, pageable);

        Page<ProductResponse> responsePage = productPage.map(ProductResponse::from);
        return PageResponse.of(responsePage);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setPublishedAt(request.getPublishedAt());
        product.setCategory(category);
        product.setTags(tags);

        Product updated = productRepository.save(product);
        log.info("Produk diupdate: {}", updated.getName());
        return ProductResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        productRepository.delete(product);
        log.info("Produk dihapus: {}", product.getName());
    }

    /**
     * Contoh native query: ambil produk termahal.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopProductsByPrice(int limit) {
        return productRepository.findTopByPriceNative(limit)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * Contoh native query: cari produk berdasarkan nama tag.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> findByTag(String tagName) {
        return productRepository.findByTagName(tagName)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
