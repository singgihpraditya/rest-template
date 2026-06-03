package com.example.template.config;

import com.example.template.entity.*;
import com.example.template.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Inisialisasi data awal untuk profile LOCAL.
 * Berjalan sekali saat aplikasi start, hanya jika data belum ada.
 *
 * Data yang dibuat:
 *   - Role: ROLE_ADMIN, ROLE_USER
 *   - User: admin (password: admin123), user (password: user123)
 *   - Category: Electronics, Clothing, Books
 *   - Tag: smartphone, laptop, fashion, java, programming
 *   - Product: beberapa contoh produk
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Data sudah ada, skip inisialisasi.");
            return;
        }

        log.info("Memulai inisialisasi data awal...");

        // 1. Buat Role
        Role adminRole = roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        Role userRole = roleRepository.save(Role.builder().name("ROLE_USER").build());

        // 2. Buat User
        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@example.com")
                .fullName("Administrator")
                .active(true)
                .roles(Set.of(adminRole, userRole))
                .build());

        userRepository.save(User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .email("user@example.com")
                .fullName("Regular User")
                .active(true)
                .roles(Set.of(userRole))
                .build());

        // 3. Buat Category
        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Produk elektronik: HP, laptop, aksesoris")
                .build());

        Category clothing = categoryRepository.save(Category.builder()
                .name("Clothing")
                .description("Pakaian dan fashion")
                .build());

        Category books = categoryRepository.save(Category.builder()
                .name("Books")
                .description("Buku fisik dan digital")
                .build());

        // 4. Buat Tag
        Tag smartphone = tagRepository.save(Tag.builder().name("smartphone").build());
        Tag laptop = tagRepository.save(Tag.builder().name("laptop").build());
        Tag apple = tagRepository.save(Tag.builder().name("apple").build());
        Tag fashion = tagRepository.save(Tag.builder().name("fashion").build());
        Tag javaProgramming = tagRepository.save(Tag.builder().name("java").build());
        Tag programming = tagRepository.save(Tag.builder().name("programming").build());

        // 5. Buat Product
        productRepository.save(Product.builder()
                .name("iPhone 15 Pro")
                .description("Smartphone flagship Apple dengan chip A17 Pro")
                .price(new BigDecimal("19999000"))
                .stock(50)
                .publishedAt(LocalDateTime.of(2023, 9, 22, 9, 0, 0))
                .category(electronics)
                .tags(Set.of(smartphone, apple))
                .build());

        productRepository.save(Product.builder()
                .name("MacBook Pro 14 M3")
                .description("Laptop profesional Apple dengan chip M3 Pro")
                .price(new BigDecimal("29999000"))
                .stock(20)
                .publishedAt(LocalDateTime.of(2023, 11, 7, 9, 0, 0))
                .category(electronics)
                .tags(Set.of(laptop, apple))
                .build());

        productRepository.save(Product.builder()
                .name("Kaos Polos Premium")
                .description("Kaos 100% katun combed 30s, tersedia berbagai warna")
                .price(new BigDecimal("75000"))
                .stock(200)
                .publishedAt(LocalDateTime.of(2024, 1, 1, 8, 0, 0))
                .category(clothing)
                .tags(Set.of(fashion))
                .build());

        productRepository.save(Product.builder()
                .name("Spring Boot in Action")
                .description("Buku panduan lengkap Spring Boot untuk developer Java")
                .price(new BigDecimal("350000"))
                .stock(100)
                .publishedAt(LocalDateTime.of(2024, 3, 15, 10, 0, 0))
                .category(books)
                .tags(Set.of(javaProgramming, programming))
                .build());

        productRepository.save(Product.builder()
                .name("Clean Code")
                .description("Buku wajib untuk setiap programmer: A Handbook of Agile Software Craftsmanship")
                .price(new BigDecimal("280000"))
                .stock(80)
                .publishedAt(LocalDateTime.of(2024, 2, 1, 10, 0, 0))
                .category(books)
                .tags(Set.of(programming))
                .build());

        log.info("Inisialisasi data selesai!");
        log.info("Login credentials: admin/admin123 | user/user123");
    }
}
