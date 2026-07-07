package com.example.template.config;

import com.example.template.entity.*;
import com.example.template.repository.*;
import com.example.template.service.EndpointPermissionService;
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
    private final EndpointPermissionRepository endpointPermissionRepository;
    private final EndpointPermissionService endpointPermissionService;
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

        // 6. Seed permission rules
        seedPermissions();

        log.info("Inisialisasi data selesai!");
        log.info("Login credentials: admin/admin123 | user/user123");
        log.info("Kelola permission di: GET /api/permissions");
    }

    /**
     * Seed aturan otorisasi ke tabel endpoint_permissions.
     * sortOrder lebih kecil = lebih prioritas (dicek lebih dulu).
     * requiredRole null/kosong = endpoint publik (tidak butuh login).
     */
    private void seedPermissions() {
        if (endpointPermissionRepository.count() > 0) {
            return;
        }

        Object[][] rules = {
            // sortOrder | httpMethod | urlPattern                  | requiredRole | description
            // --- Endpoint selalu publik (sistem) ---
            {  1, "*",    "/error",                       null,          "Error handler Spring Boot" },
            {  2, "*",    "/api/auth/**",                 null,          "Login & register (publik)" },
            {  3, "*",    "/v3/api-docs/**",              null,          "Swagger API docs" },
            {  4, "*",    "/swagger-ui/**",               null,          "Swagger UI" },
            {  5, "*",    "/swagger-ui.html",             null,          "Swagger UI HTML" },
            {  6, "*",    "/h2-console/**",               null,          "H2 Console (local only)" },
            {  7, "GET",  "/actuator/health",             null,          "Health check" },
            {  8, "GET",  "/files/**",                    null,          "Static file access" },

            // --- Endpoint publik (read only) ---
            {  10, "GET", "/api/products/**",             null,          "Baca produk (publik)" },
            {  11, "GET", "/api/categories/**",           null,          "Baca kategori (publik)" },
            {  12, "GET", "/api/external/**",             null,          "Demo Feign Client (publik)" },

            // --- Hanya ADMIN ---
            {  20, "POST",   "/api/categories",           "ROLE_ADMIN",  "Buat kategori baru" },
            {  21, "PUT",    "/api/categories/**",        "ROLE_ADMIN",  "Update kategori" },
            {  22, "DELETE", "/api/categories/**",        "ROLE_ADMIN",  "Hapus kategori" },
            {  23, "POST",   "/api/products",             "ROLE_ADMIN",  "Buat produk baru" },
            {  24, "PUT",    "/api/products/**",          "ROLE_ADMIN",  "Update produk" },
            {  25, "DELETE", "/api/products/**",          "ROLE_ADMIN",  "Hapus produk" },
            {  26, "*",      "/api/permissions/**",       "ROLE_ADMIN",  "Kelola permission (ADMIN)" },
            {  27, "*",      "/api/cache/**",             "ROLE_ADMIN",  "Kelola cache (ADMIN)" },

            // --- User yang sudah login (role apapun) ---
            {  30, "*",      "/api/files/**",             "ROLE_USER",   "Upload/download file" },
            {  31, "GET",    "/api/auth/me",              "ROLE_USER",   "Get current user info" },

            // --- Diagnostic (publik, untuk verifikasi tracing) ---
            {  13, "GET",    "/api/diagnostic/**",         null,          "Endpoint verifikasi tracing (publik)" },

            // --- Default fallback: wajib login ---
            { 999, "*",      "/api/**",                   "ROLE_USER",   "Default: semua /api/** wajib login" },
        };

        for (Object[] r : rules) {
            endpointPermissionRepository.save(EndpointPermission.builder()
                    .sortOrder((Integer) r[0])
                    .httpMethod((String) r[1])
                    .urlPattern((String) r[2])
                    .requiredRole((String) r[3])
                    .description((String) r[4])
                    .active(true)
                    .build());
        }

        // Refresh cache setelah semua rule disimpan
        endpointPermissionService.refresh();
        log.info("Permission rules berhasil di-seed: {} rules", rules.length);
    }
}
