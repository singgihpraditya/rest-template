package com.example.template.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity untuk menyimpan aturan otorisasi per endpoint.
 *
 * Setiap row mendefinisikan: "request dengan method X ke URL Y membutuhkan role Z".
 *
 * Contoh:
 *   httpMethod="POST",  urlPattern="/api/categories/**", requiredRole="ROLE_ADMIN"
 *   httpMethod="GET",   urlPattern="/api/products/**",   requiredRole=null  (publik)
 *   httpMethod="*",     urlPattern="/api/auth/**",       requiredRole=null  (publik, semua method)
 *
 * sortOrder: rule dengan sortOrder lebih kecil dicek lebih dulu (lebih spesifik = angka lebih kecil).
 */
@Entity
@Table(name = "endpoint_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointPermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // HTTP method: GET, POST, PUT, DELETE, atau "*" untuk semua method
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    // Pola URL menggunakan Ant-style: /api/categories/**, /api/products/*, dll.
    @Column(name = "url_pattern", nullable = false, length = 200)
    private String urlPattern;

    // Role yang dibutuhkan: ROLE_ADMIN, ROLE_USER, atau null/kosong untuk endpoint publik
    @Column(name = "required_role", length = 50)
    private String requiredRole;

    // Prioritas pencocokan: angka lebih kecil = dicek lebih dulu
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // Rule bisa dinonaktifkan tanpa dihapus
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "description", length = 255)
    private String description;
}
