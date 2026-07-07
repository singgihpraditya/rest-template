# REST Template — Panduan Developer

Kamu adalah senior Java developer. Dokumen ini adalah **sumber tunggal kebenaran** untuk project ini.
Baca seluruh dokumen sebelum membuat atau mengubah kode.

---

## 1. Tech Stack & Versi

| Teknologi | Versi | Catatan |
|-----------|-------|---------|
| Java | 21 | |
| Spring Boot | 3.2.3 | Jakarta EE (bukan javax.*) |
| Spring Security | 6.x | JWT Stateless |
| Spring Data JPA | via Boot | H2 (local), PostgreSQL (dev/prod) |
| Spring Cloud OpenFeign | 2023.0.0 | Pemanggilan REST API eksternal |
| Lombok | via Boot | Wajib di semua class |
| JJWT | 0.12.3 | `Jwts.parser().verifyWith().build()` |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI di `/swagger-ui.html` |
| Log4j2 | via Boot | Gantikan Logback |
| Micrometer Tracing | via Boot | Bridge OTel, traceId di MDC |
| Maven | - | Build tool |

---

## 2. Struktur Package

```
com.example.template/
├── aspect/
│   └── LoggingAspect.java          ← AOP: log entry/exit/durasi semua method controller
├── config/
│   ├── CacheConfig.java             ← Konstanta nama cache (CATEGORIES_WITH_PRODUCT_COUNT_CACHE)
│   ├── DataInitializer.java         ← Seed data awal (hanya profile local)
│   ├── JacksonConfig.java           ← Global: snake_case + format LocalDateTime
│   ├── OpenApiConfig.java           ← Swagger UI + JWT auth schema
│   ├── SecurityConfig.java          ← Spring Security + DynamicAuthorizationManager
│   ├── TraceIdLoggingFilter.java    ← Isi traceId + requestId ke MDC setiap HTTP request
│   └── WebMvcConfig.java            ← Static resource handler untuk /files/**
├── controller/
│   ├── AuthController.java          ← POST /api/auth/login, register, GET /api/auth/me
│   ├── CacheController.java         ← POST /api/cache/cleanup (ADMIN) — bersihkan semua cache
│   ├── CategoryController.java      ← CRUD /api/categories + GET /api/categories/stats
│   ├── DiagnosticController.java    ← GET /api/diagnostic/trace — verifikasi OTel tracing
│   ├── EndpointPermissionController.java ← CRUD /api/permissions (ADMIN)
│   ├── ExternalApiController.java   ← GET /api/external/** (demo Feign)
│   ├── FileController.java          ← POST /api/files/upload, GET /api/files/download/**
│   └── ProductController.java       ← CRUD /api/products
├── dto/
│   ├── request/                     ← Input dari client (@Getter @NoArgsConstructor)
│   │   ├── CategoryRequest.java
│   │   ├── EndpointPermissionRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ProductRequest.java
│   │   └── RegisterRequest.java
│   └── response/                    ← Output ke client (@Getter @Builder + static from())
│       ├── ApiResponse.java         ← Envelope semua response
│       ├── CategoryResponse.java
│       ├── EndpointPermissionResponse.java
│       ├── ErrorMessage.java
│       ├── ErrorSchema.java
│       ├── PageResponse.java        ← Pagination custom (bukan Spring Page)
│       ├── PostResponse.java        ← DTO untuk API eksternal JSONPlaceholder
│       ├── ProductResponse.java
│       ├── TagResponse.java
│       ├── TokenResponse.java
│       └── UserResponse.java
├── entity/
│   ├── BaseEntity.java              ← @MappedSuperclass: createdAt, updatedAt (auto)
│   ├── Category.java                ← One-to-Many → Product
│   ├── EndpointPermission.java      ← Aturan otorisasi dinamis
│   ├── Product.java                 ← Many-to-One ← Category; Many-to-Many ↔ Tag
│   ├── Role.java                    ← Many-to-Many ↔ User
│   ├── Tag.java                     ← Many-to-Many ↔ Product
│   └── User.java                    ← Many-to-Many → Role
├── exception/
│   ├── BusinessException.java       ← 409 Conflict
│   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice, tangani semua exception
│   └── ResourceNotFoundException.java ← 404 Not Found
├── feign/
│   └── JsonPlaceholderClient.java   ← @FeignClient ke jsonplaceholder.typicode.com
├── repository/
│   ├── projection/
│   │   └── CategoryProductCountProjection.java ← Interface projection untuk query native (id, name, description, productCount)
│   ├── CategoryRepository.java      ← Contoh: native query GROUP BY, dikembalikan sebagai Projection
│   ├── EndpointPermissionRepository.java
│   ├── ProductRepository.java       ← Contoh: native query JOIN, LIMIT
│   ├── RoleRepository.java
│   ├── TagRepository.java
│   └── UserRepository.java
├── security/
│   ├── DynamicAuthorizationManager.java ← Baca rule dari DB, bukan hardcoded
│   ├── JwtAccessDeniedHandler.java  ← 403 → format ApiResponse
│   ├── JwtAuthenticationEntryPoint.java ← 401 → format ApiResponse
│   ├── JwtAuthenticationFilter.java ← Validasi Bearer token setiap request
│   ├── JwtTokenProvider.java        ← Generate & validasi JWT (JJWT 0.12.x)
│   └── UserDetailsServiceImpl.java  ← Load user dari DB untuk Spring Security
└── service/
    ├── AuthService.java             ← login(), register(), getCurrentUser()
    ├── CategoryService.java
    ├── EndpointPermissionService.java ← CRUD + in-memory cache permission rules
    ├── ExternalApiService.java      ← Wrapper Feign client
    ├── FileStorageService.java      ← Simpan & load file dari disk
    └── ProductService.java
```

---

## 3. Konvensi Kode

### 3.1 Entity

```java
@Entity
@Table(name = "nama_tabel")   // selalu eksplisit
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor   // keempat-empatnya wajib
public class NamaEntity extends BaseEntity {      // selalu extend BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nama_kolom", nullable = false, length = 100)
    private String namaField;

    @Builder.Default                              // wajib untuk field dengan nilai default
    private boolean active = true;
}
```

- **Selalu extend `BaseEntity`** → dapat `createdAt` dan `updatedAt` otomatis
- **Nama tabel** selalu plural snake_case: `categories`, `products`, `endpoint_permissions`
- **Nama kolom** selalu snake_case via `@Column(name = "...")`
- **`@Builder.Default`** wajib jika field punya nilai default (boolean, List, Set, dll.)

**Relasi:**
```java
// Many-to-One (sisi yang punya foreign key)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;

// One-to-Many (sisi tanpa FK, mappedBy ke field di sisi ManyToOne)
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private List<Product> products = new ArrayList<>();

// Many-to-Many (sisi aktif yang punya @JoinTable)
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(name = "product_tags",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"))
@Builder.Default
private Set<Tag> tags = new HashSet<>();
```

---

### 3.2 Repository

```java
@Repository
public interface NamaRepository extends JpaRepository<Nama, Long> {

    // JPQL Query
    @Query("SELECT n FROM Nama n WHERE LOWER(n.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Nama> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    // Native Query
    @Query(value = "SELECT * FROM nama_tabel WHERE active = true ORDER BY created_at DESC LIMIT :limit",
           nativeQuery = true)
    List<Nama> findTopActiveNative(@Param("limit") int limit);

    boolean existsByName(String name);
}
```

- Extend `JpaRepository<Entity, Long>`
- JPQL untuk query sederhana, native query untuk JOIN kompleks / SQL spesifik DB

**Native Query dengan Projection (WAJIB — bukan `List<Object[]>`):**
```java
// Buat interface projection di package repository/projection/
public interface NamaProjection {
    Long getId();
    String getName();
    Long getCount();
}

// Repository: return type pakai projection, BUKAN Object[]
@Query(value = "SELECT id, name, COUNT(*) AS count FROM ...", nativeQuery = true)
List<NamaProjection> findWithCount();
```
Gunakan projection (bukan `List<Object[]>`) untuk native query agar type-safe dan mudah di-test.

---

### 3.3 DTO

**Request DTO** (menerima input dari client):
```java
@Getter
@NoArgsConstructor
public class NamaRequest {                        // TIDAK ada @Builder, TIDAK ada @JsonNaming

    @NotBlank(message = "Nama tidak boleh kosong")
    @Size(max = 100, message = "Nama maks 100 karakter")
    private String name;

    @NotNull(message = "ID tidak boleh kosong")
    private Long namaId;                          // akan diterima dari JSON sebagai "nama_id"
}
```

**Response DTO** (mengembalikan data ke client):
```java
@Getter
@Builder
public class NamaResponse {                       // TIDAK ada @JsonNaming

    private Long id;
    private String name;
    private LocalDateTime createdAt;              // JSON: "created_at": "2024-01-15 10:30:00"

    public static NamaResponse from(Nama entity) {   // selalu ada static factory method
        return NamaResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
```

**Kenapa tidak butuh `@JsonNaming`?**
`JacksonConfig.java` sudah mengatur `PropertyNamingStrategies.SNAKE_CASE` secara global.
Java field `namaId` → JSON `nama_id` otomatis, tanpa annotasi.

**Format LocalDateTime:** `"yyyy-MM-dd HH:mm:ss"` — dikonfigurasi di `JacksonConfig` via `JavaTimeModule`.

**Pengecualian:** `PostResponse` (DTO dari API eksternal JSONPlaceholder) menggunakan `@JsonProperty` eksplisit
karena API eksternal mengembalikan format berbeda (camelCase).

---

### 3.4 Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NamaService {

    private final NamaRepository namaRepository;

    @Transactional(readOnly = true)              // selalu readOnly untuk operasi baca
    public NamaResponse findById(Long id) {
        Nama nama = namaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nama", id));
        return NamaResponse.from(nama);
    }

    @Transactional(readOnly = true)
    public PageResponse<NamaResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Nama> result = namaRepository.findAll(pageable);
        return PageResponse.of(result.map(NamaResponse::from));
    }

    @Transactional                               // wajib untuk operasi tulis
    public NamaResponse create(NamaRequest request) {
        if (namaRepository.existsByName(request.getName())) {
            throw new BusinessException("Nama '" + request.getName() + "' sudah ada");
        }
        Nama saved = namaRepository.save(Nama.builder()
                .name(request.getName())
                .build());
        log.info("Nama baru dibuat: {}", saved.getName());
        return NamaResponse.from(saved);
    }
}
```

**Exception yang digunakan:**

| Exception | Error Code | HTTP | Kapan dipakai |
|-----------|-----------|------|--------------|
| `ResourceNotFoundException("Entity", id)` | RST-002 | 404 | Data tidak ditemukan di DB |
| `BusinessException("pesan")` | RST-003 | 409 | Duplikat, data tidak bisa dihapus, stok habis, dll. |
| `MethodArgumentNotValidException` | RST-001 | 400 | Otomatis dari `@Valid` di controller |

Semua exception ditangani di `GlobalExceptionHandler.java` — tidak perlu try-catch di service.

---

### 3.5 Controller

```java
@RestController
@RequestMapping("/api/nama")
@RequiredArgsConstructor
@Tag(name = "Nama", description = "Deskripsi endpoint")
@SecurityRequirement(name = "bearerAuth")        // tambahkan jika endpoint butuh login
public class NamaController {

    private final NamaService namaService;

    @GetMapping
    @Operation(summary = "List Nama", description = "Ambil semua dengan pagination")
    public ResponseEntity<ApiResponse<PageResponse<NamaResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(namaService.findAll(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail Nama")
    public ResponseEntity<ApiResponse<NamaResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(namaService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Tambah Nama")
    public ResponseEntity<ApiResponse<NamaResponse>> create(@Valid @RequestBody NamaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(namaService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Nama")
    public ResponseEntity<ApiResponse<NamaResponse>> update(
            @PathVariable Long id, @Valid @RequestBody NamaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(namaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus Nama")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        namaService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

**Pola return value:**
- **Sukses:** `ResponseEntity.ok(ApiResponse.success(data))`
- **Create:** `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data))`
- **Delete:** `ResponseEntity.ok(ApiResponse.success(null))`

---

## 4. Format Response API

### Response Sukses
```json
{
  "error_schema": {
    "error_code": "RST-000",
    "error_message": { "english": "Success", "indonesian": "Berhasil" }
  },
  "output_schema": {
    "id": 1,
    "name": "Contoh",
    "created_at": "2024-01-15 10:30:00"
  }
}
```

### Response Pagination
```json
{
  "error_schema": { "error_code": "RST-000", "error_message": { ... } },
  "output_schema": {
    "content": [ ... ],
    "page_number": 0,
    "page_size": 10,
    "total_elements": 25,
    "total_pages": 3,
    "first": true,
    "last": false
  }
}
```

### Response Error
```json
{
  "error_schema": {
    "error_code": "RST-002",
    "error_message": {
      "english": "Product dengan ID 99 tidak ditemukan",
      "indonesian": "Product dengan ID 99 tidak ditemukan"
    }
  }
}
```

---

## 5. Sistem Otorisasi Dinamis

Aturan akses **tidak hardcoded** — disimpan di tabel `endpoint_permissions` dan dapat diubah tanpa restart.

### Cara Kerja
```
Request → JwtAuthenticationFilter (validasi token)
        → DynamicAuthorizationManager
        → EndpointPermissionService (cek cache)
        → Cocokkan [httpMethod + urlPattern] dengan sortOrder terkecil dulu
        → requiredRole null/kosong → PUBLIK
        → requiredRole ada → cek authorities user
```

### Struktur Rule di DB
| Field | Nilai contoh | Keterangan |
|-------|-------------|-----------|
| `http_method` | `GET`, `POST`, `*` | `*` = semua method |
| `url_pattern` | `/api/products/**` | Ant-style pattern |
| `required_role` | `ROLE_ADMIN`, `ROLE_USER`, null | null = publik |
| `sort_order` | `1` s/d `999` | Lebih kecil = lebih prioritas |
| `active` | `true`/`false` | Nonaktifkan tanpa hapus |

### Menambah Rule untuk Endpoint Baru
Setelah membuat controller baru, daftarkan permission-nya:
```bash
POST /api/permissions
Authorization: Bearer <admin-token>

{
  "http_method": "GET",
  "url_pattern": "/api/orders/**",
  "required_role": "ROLE_USER",
  "sort_order": 50,
  "description": "Baca order (user yang login)"
}
```

### Rule Default yang Sudah Ada
| sort_order | Method | Pattern | Role | Keterangan |
|-----------|--------|---------|------|-----------|
| 1 | `*` | `/error` | - | Error handler |
| 2 | `*` | `/api/auth/**` | - | Login & register |
| 3-7 | `*` | Swagger, H2, actuator | - | Dev tools |
| 10 | `GET` | `/api/products/**` | - | Publik |
| 11 | `GET` | `/api/categories/**` | - | Publik |
| 12 | `GET` | `/api/external/**` | - | Publik |
| 13 | `GET` | `/api/diagnostic/**` | - | Publik (diagnostic) |
| 20-25 | `POST/PUT/DELETE` | `/api/categories/**`, `/api/products/**` | ROLE_ADMIN | Admin only |
| 26 | `*` | `/api/permissions/**` | ROLE_ADMIN | Kelola permission |
| 27 | `*` | `/api/cache/**` | ROLE_ADMIN | Cache management |
| 30 | `*` | `/api/files/**` | ROLE_USER | Upload file |
| 999 | `*` | `/api/**` | ROLE_USER | Default fallback |

---

## 6. Langkah Membuat Fitur CRUD Baru

Contoh: membuat fitur **Order Management**.

### Step 1 — Entity
```java
// src/main/java/com/example/template/entity/Order.java
@Entity @Table(name = "orders")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Order extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    // ... field lainnya
}
```

### Step 2 — Repository
```java
// src/main/java/com/example/template/repository/OrderRepository.java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
}
```

### Step 3 — Request DTO
```java
// src/main/java/com/example/template/dto/request/OrderRequest.java
@Getter @NoArgsConstructor
public class OrderRequest {
    @NotBlank private String orderNumber;
    @NotNull private Long userId;
}
```

### Step 4 — Response DTO
```java
// src/main/java/com/example/template/dto/response/OrderResponse.java
@Getter @Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
```

### Step 5 — Service
Ikuti pola di section 3.4. Gunakan `@Transactional(readOnly=true)` untuk reads,
`@Transactional` untuk writes. Lempar `ResourceNotFoundException` atau `BusinessException`.

### Step 6 — Controller
Ikuti pola di section 3.5. Tambahkan `@Tag`, `@Operation`, `@SecurityRequirement`.

### Step 7 — Daftarkan Permission
```bash
POST /api/permissions  (sebagai admin)
{ "http_method": "GET",    "url_pattern": "/api/orders/**",  "required_role": "ROLE_USER",  "sort_order": 40 }
{ "http_method": "POST",   "url_pattern": "/api/orders",     "required_role": "ROLE_USER",  "sort_order": 41 }
{ "http_method": "PUT",    "url_pattern": "/api/orders/**",  "required_role": "ROLE_ADMIN", "sort_order": 42 }
{ "http_method": "DELETE", "url_pattern": "/api/orders/**",  "required_role": "ROLE_ADMIN", "sort_order": 43 }
```

---

## 7. Pemanggilan API Eksternal (Feign Client)

```java
// src/main/java/com/example/template/feign/NamaApiClient.java
@FeignClient(name = "nama-api", url = "${external.api.nama.url}")
public interface NamaApiClient {
    @GetMapping("/endpoint")
    List<NamaResponse> getData();
}
```

Tambahkan URL di `application.properties`:
```properties
external.api.nama.url=https://api.example.com
```

DTO untuk external API: gunakan `@JsonProperty` eksplisit karena tidak ikut konvensi snake_case internal.

---

## 8. Konfigurasi per Profile

| Property | local | dev | prod |
|----------|-------|-----|------|
| Database | H2 in-memory | PostgreSQL | PostgreSQL |
| DB Name | `logitrackdb` | `rest_template_dev` | `rest_template_prod` (via env var) |
| `ddl-auto` | `create-drop` | `update` | `validate` |
| SQL logging | enabled | enabled | disabled |
| H2 Console | enabled | disabled | disabled |
| Log level app | DEBUG | DEBUG | INFO |

**Credential prod via environment variable:**
```bash
DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
```

**JWT:** secret Base64, expiration 10 menit (600000ms), claims: `sub` (username) + `roles` (list).

---

## 9. Logging & Tracing

- **`LoggingAspect`**: log otomatis `[ENTRY]`, `[EXIT]`, durasi untuk semua method controller
- **`TraceIdLoggingFilter`**: isi `traceId`, `spanId`, dan `requestId` ke MDC setiap HTTP request
- **Log pattern:** `timestamp | level | thread | logger | traceId=... | requestId=... | pesan`
- Log startup/background menampilkan `traceId=NO_TRACE | requestId=NO_REQ` (tidak ada HTTP request aktif)
- File log: `logs/app.log`, rotate harian + 10MB, history 30 hari

### traceId vs requestId

| | `traceId` | `requestId` |
|---|---|---|
| Dibuat oleh | Micrometer (server, otomatis) | Client via header, atau UUID otomatis |
| Dikontrol client? | Tidak | Ya, via header `X-Request-Id` |
| Tujuan | Debug distributed tracing lintas service (Jaeger) | Korelasi log antara client dan server |
| Format | Hex 32 karakter (W3C standard) | Bebas — UUID, sequence number, dll. |

**Cara kirim requestId dari Postman / UI:**
```
Header: X-Request-Id: checkout-retry-3
```
Jika header tidak dikirim, server generate UUID otomatis. Nilai requestId selalu dikembalikan di response header `X-Request-Id`.

### Verifikasi Tracing Berjalan

**Cara 1 — Log (tanpa setup tambahan):**
```
# Hit sembarang endpoint, lalu cek log
GET /api/categories
→ Log harus mengandung:
  traceId=4bf92f3577b34da6a3ce929d0e0e4736 (hex 32 char)
  requestId=550e8400-e29b-41d4-a716-446655440000 (UUID atau nilai dari header)
→ Jika masih traceId=NO_TRACE → ada masalah dengan Micrometer
```

**Cara 2 — Endpoint diagnostik (tanpa setup tambahan):**
```
GET /api/diagnostic/trace
→ status: "OK", trace_id: "4bf92f3577b34da6a3ce929d0e0e4736"
→ mdc_trace_id harus sama dengan trace_id
```

**Cara 3 — Jaeger UI (visual, butuh Docker):**
```bash
# 1. Uncomment di pom.xml: opentelemetry-exporter-otlp
# 2. Uncomment di application-local.properties:
#    management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
# 3. Jalankan Jaeger
docker-compose up -d
# 4. Buka http://localhost:16686 → pilih service "rest-template"
```

---

## 10. Data Default (Profile Local)

Dibuat otomatis oleh `DataInitializer.java` saat startup:

| Type | Data |
|------|------|
| User | `admin / admin123` → ROLE_ADMIN + ROLE_USER |
| User | `user / user123` → ROLE_USER |
| Category | Electronics, Clothing, Books |
| Product | iPhone 15 Pro, MacBook Pro, Kaos Polos, Spring Boot in Action, Clean Code |
| Tag | smartphone, laptop, apple, fashion, java, programming |

---

## 11. File Penting

| File | Fungsi |
|------|--------|
| `pom.xml` | Dependencies Maven + JaCoCo coverage plugin |
| `src/main/resources/application.properties` | Base config (semua profile) |
| `src/main/resources/application-local.properties` | Config H2, dev tools |
| `src/main/resources/application-dev.properties` | Config PostgreSQL dev |
| `src/main/resources/application-prod.properties` | Config PostgreSQL prod |
| `src/main/resources/log4j2.xml` | Konfigurasi Log4j2 |
| `rest-template.postman_collection.json` | Postman collection semua endpoint |
| `target/site/jacoco/index.html` | Laporan coverage JaCoCo (dibuat setelah `mvn verify`) |

**Cara menjalankan:**
```bash
# Profile local (H2, seed data otomatis)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Swagger UI
http://localhost:8080/swagger-ui.html

# H2 Console (local only)
http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:logitrackdb)
```

**Menjalankan unit test dan laporan coverage:**
```bash
# Jalankan semua test + generate laporan JaCoCo
mvn test -Dspring.profiles.active=local

# Verifikasi coverage 100% pada controller & service (build gagal jika tidak terpenuhi)
mvn verify -Dspring.profiles.active=local

# Buka laporan HTML coverage
target/site/jacoco/index.html
```

---

## 12. Hal yang Harus Diikuti

1. **Selalu gunakan `ApiResponse<T>`** sebagai wrapper response — tidak boleh return raw object
2. **Selalu extend `BaseEntity`** — jangan tulis `createdAt`/`updatedAt` manual
3. **Jangan pakai `@JsonNaming`** di DTO — sudah global via `JacksonConfig`
4. **Jangan hardcode rule otorisasi** di `SecurityConfig` — tambahkan via `/api/permissions`
5. **`@Transactional(readOnly=true)`** untuk semua operasi baca
6. **Lempar exception yang benar:** `ResourceNotFoundException` untuk 404, `BusinessException` untuk 409
7. **Format timestamp** input/output: `"yyyy-MM-dd HH:mm:ss"` — bukan ISO-8601
8. **Nama file Java harus sama persis** dengan nama class public di dalamnya
9. **Native query WAJIB pakai Projection** — bukan `List<Object[]>`. Buat interface di `repository/projection/`
10. **Nama cache WAJIB pakai konstanta** dari `CacheConfig` — bukan string literal langsung di `@Cacheable`

---

## 13. Unit Testing

### Strategi
- **Framework:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- **Pendekatan:** Pure unit test — mock semua dependency, **tidak** start Spring context
- **Target coverage:** 100% instruction, method, dan branch pada package `controller` dan `service`
- **Enforced oleh:** JaCoCo `check` goal saat `mvn verify`

### Struktur Test

```
src/test/java/com/example/template/
├── controller/
│   ├── AuthControllerTest.java
│   ├── CacheControllerTest.java
│   ├── CategoryControllerTest.java
│   ├── DiagnosticControllerTest.java
│   ├── EndpointPermissionControllerTest.java
│   ├── ExternalApiControllerTest.java
│   ├── FileControllerTest.java
│   └── ProductControllerTest.java
└── service/
    ├── AuthServiceTest.java
    ├── CategoryServiceTest.java
    ├── EndpointPermissionServiceTest.java
    ├── ExternalApiServiceTest.java
    ├── FileStorageServiceTest.java
    └── ProductServiceTest.java
```

### Pola Penulisan Test

**1. Setup dasar (semua test class):**
```java
@ExtendWith(MockitoExtension.class)
class NamaServiceTest {
    @Mock private NamaRepository namaRepository;
    @InjectMocks private NamaService namaService;
}
```

**2. Set field pada Request DTO** (karena hanya punya `@NoArgsConstructor`, tanpa setter/builder):
```java
NamaRequest request = new NamaRequest();
ReflectionTestUtils.setField(request, "name", "nilai");
ReflectionTestUtils.setField(request, "namaId", 1L);
```

**3. Set `@Value` field pada Service:**
```java
// Wajib dilakukan di @BeforeEach untuk service yang punya @Value
ReflectionTestUtils.setField(authService, "jwtExpiration", 600000L);
```

**4. Controller dengan `@AuthenticationPrincipal`** — langsung pass mock UserDetails:
```java
UserDetails mockUserDetails = mock(UserDetails.class);
when(mockUserDetails.getUsername()).thenReturn("admin");
controller.getCurrentUser(mockUserDetails);
```

**5. Controller dengan `ServletUriComponentsBuilder`** (FileController) — setup RequestContextHolder:
```java
@BeforeEach
void setUp() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setScheme("http"); req.setServerName("localhost"); req.setServerPort(8080);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
}
@AfterEach void tearDown() { RequestContextHolder.resetRequestAttributes(); }
```

**6. Service dengan `@PostConstruct`** — `@PostConstruct` TIDAK dipanggil oleh Mockito.
Panggil manual jika diperlukan, atau set field via `ReflectionTestUtils`:
```java
// FileStorageService: panggil init() manual setelah set uploadDir
ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
service.init();

// EndpointPermissionService: cache permissionCache mulai kosong → otomatis test lazy-load
```

**7. Mock static method** (misal `Files.createDirectories`):
```java
try (MockedStatic<Files> mockFiles = mockStatic(Files.class)) {
    mockFiles.when(() -> Files.createDirectories(any())).thenThrow(new IOException());
    assertThrows(RuntimeException.class, service::init);
}
```

**8. Mock konstruktor** (misal `new UrlResource(...)`):
```java
try (MockedConstruction<UrlResource> mocked = mockConstruction(UrlResource.class,
        (mock, context) -> {
            when(mock.exists()).thenReturn(true);
            when(mock.isReadable()).thenReturn(false);
        })) {
    assertThrows(ResourceNotFoundException.class, () -> service.loadFileAsResource("file.txt"));
}
```

**9. Test cache in-memory** (`EndpointPermissionService.permissionCache`):
```java
// Pre-populate cache untuk test "cache hit"
ReflectionTestUtils.setField(service, "permissionCache", List.of(permission));

// Biarkan cache kosong untuk test lazy-load (Mockito tidak panggil @PostConstruct)
when(repository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(perm));
service.findMatchingPermission("GET", "/api/test"); // otomatis memanggil refresh()
```

### Aturan Coverage 100%

Semua branch harus dicover, termasuk:
- Setiap `if/else` → test satu untuk tiap kondisi
- `try/catch` → test happy path DAN exception path
- Compound condition (`&&`, `||`) → test short-circuit evaluation
- Lambdas di `orElseThrow()` → test kondisi empty Optional
- Loop kosong vs loop berisi (untuk `CacheController`)
