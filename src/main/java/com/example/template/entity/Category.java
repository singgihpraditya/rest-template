package com.example.template.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Category: kategori produk.
 * Relasi:
 *   - One-to-Many dengan Product (satu kategori punya banyak produk)
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // Relasi One-to-Many dengan Product
    // mappedBy merujuk ke field "category" di class Product
    // CascadeType.ALL: operasi (persist, remove, dll.) diteruskan ke child
    // orphanRemoval: hapus Product jika dilepas dari Category
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
