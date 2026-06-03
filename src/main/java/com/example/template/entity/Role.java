package com.example.template.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity Role: menyimpan data role/hak akses pengguna.
 * Relasi: Many-to-Many dengan User (mappedBy ada di User).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name; // Contoh: "ROLE_ADMIN", "ROLE_USER"

    // Relasi Many-to-Many ke User (sisi pasif, mappedBy di User)
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();
}
