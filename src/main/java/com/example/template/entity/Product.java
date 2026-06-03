package com.example.template.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity Product: data produk.
 * Relasi:
 *   - Many-to-One dengan Category (banyak produk dalam satu kategori)
 *   - Many-to-Many dengan Tag (produk bisa punya banyak tag, tag bisa ada di banyak produk)
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private int stock = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Timestamp kapan produk mulai dipublikasikan / tersedia untuk dijual (nullable)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Relasi Many-to-One dengan Category
    // Satu produk hanya bisa masuk ke satu kategori
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Relasi Many-to-Many dengan Tag
    // @JoinTable mendefinisikan tabel join "product_tags"
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_tags",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}
