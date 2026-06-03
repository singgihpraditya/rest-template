package com.example.template.repository;

import com.example.template.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- JPQL Query ---
    // Cari produk berdasarkan category id dengan pagination
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // Cari produk berdasarkan nama (case-insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    // --- Native Query #1 ---
    // Cari produk berdasarkan kategori dengan LIMIT (menggunakan native SQL)
    // Berguna ketika tidak perlu full pagination, hanya ambil N record teratas
    @Query(value = "SELECT * FROM products WHERE category_id = :categoryId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Product> findTopByCategoryIdNative(@Param("categoryId") Long categoryId, @Param("limit") int limit);

    // --- Native Query #2 ---
    // Ambil produk dengan harga tertinggi (contoh ORDER BY dengan native query)
    @Query(value = "SELECT * FROM products ORDER BY price DESC LIMIT :limit", nativeQuery = true)
    List<Product> findTopByPriceNative(@Param("limit") int limit);

    // --- Native Query #3 ---
    // Cari produk berdasarkan tag (JOIN ke tabel product_tags)
    @Query(value = """
            SELECT DISTINCT p.* FROM products p
            INNER JOIN product_tags pt ON pt.product_id = p.id
            INNER JOIN tags t ON t.id = pt.tag_id
            WHERE LOWER(t.name) = LOWER(:tagName)
            """, nativeQuery = true)
    List<Product> findByTagName(@Param("tagName") String tagName);
}
