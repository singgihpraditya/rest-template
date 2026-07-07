package com.example.template.repository;

import com.example.template.entity.Category;
import com.example.template.repository.projection.CategoryProductCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // --- JPQL Query ---
    // Cari kategori berdasarkan nama (case-insensitive, partial match)
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Category> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    // --- Native Query #1 ---
    // Mendapatkan semua kategori beserta jumlah produk di masing-masing kategori
    // Ini contoh penggunaan native query dengan JOIN dan GROUP BY
    @Query(value = """
            SELECT c.id, c.name, c.description, COUNT(p.id) AS product_count
            FROM categories c
            LEFT JOIN products p ON p.category_id = c.id
            GROUP BY c.id, c.name, c.description
            ORDER BY product_count DESC
            """, nativeQuery = true)
    List<CategoryProductCountProjection> findCategoriesWithProductCount();

    // --- Native Query #2 ---
    // Cari kategori berdasarkan keyword menggunakan native SQL LIKE
    @Query(value = "SELECT * FROM categories WHERE LOWER(name) LIKE LOWER(CONCAT('%', :keyword, '%'))", nativeQuery = true)
    List<Category> searchByNameNative(@Param("keyword") String keyword);

    boolean existsByName(String name);
}
