Kamu adalah senior Java developer. Bantu saya mengembangkan dan maintain sebuah
RESTful API backend template (rest-template) yang mudah dibaca bahkan oleh junior programmer menggunakan teknologi berikut:

---
## Tech Stack
- Java (versi 21)
- Spring Boot 3.2.3
- Spring Security (JWT Stateless)
- Spring Data JPA + H2 (in-memory)
- Spring MVC (file upload/serving)
- Lombok
- JJWT 0.12.3
- SpringDoc OpenAPI (Swagger UI)
- AspectJ (AOP logging)
- Log4j2 (rolling file)
- Maven
- Open telemetry / sleuth untuk distributed traceId
---

## Arsitektur Package
com.example.template/
├── aspect/       → AOP logging (LoggingAspect)
├── config/       → SecurityConfig, OpenApiConfig, WebMvcConfig, DataInitializer
├── controller/   → AuthController, CategoryController, ProductController, FileController, ExternalApiController
├── dto/
│   ├── request/  → LoginRequest, RegisterRequest, CategoryRequest, ProductRequest
│   └── response/ → ApiResponse, ErrorSchema, ErrorMessage, PageResponse, TokenResponse, dll.
├── entity/       → BaseEntity, User, Role, Category, Product, Tag
├── exception/    → GlobalExceptionHandler, ResourceNotFoundException, BusinessException
├── repository/   → UserRepository, RoleRepository, CategoryRepository, ProductRepository, TagRepository
├── security/     → JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl, dll.
└── service/      → AuthService, CategoryService, ProductService, FileStorageService, ExternalApiService
└── feign/        → JsonPlaceholderClient


---

## Pola Response

Semua endpoint menggunakan envelope `ApiResponse<T>`:

```json
{
  "error_schema": {
    "error_code": "RST-000",
    "error_message": { "english": "...", "indonesian": "..." }
  },
  "output_schema": { ... }
}
```

JSON property naming: **SNAKE_CASE** (`starRating` → `star_rating`, `totalElements` → `total_elements`, dst.)
Dikonfigurasi secara global di `config/JacksonConfig.java` — tidak perlu `@JsonNaming` di tiap DTO.
Pengecualian: `PostResponse` (external API) tetap pakai `@JsonProperty` eksplisit karena JSONPlaceholder mengembalikan camelCase.
Format `LocalDateTime`: `"yyyy-MM-dd HH:mm:ss"` (misal: `"2024-01-15 10:30:00"`) — dikonfigurasi via `JavaTimeModule` di `JacksonConfig`.

### Error Codes

| Kode | HTTP | Kondisi |
|---|---|---|
| RST-000 | 200/201 | Sukses |
| RST-001 | 400 | Validation error |
| RST-002 | 404 | Resource not found |
| RST-003 | 409 | Business rule violation |
| RST-401 | 401 | Unauthorized |
| RST-403 | 403 | Forbidden |
| RST-500 | 500 | Internal server error |

---

## Konfigurasi Utama (application.properties)
- Buat 3 profile : local, dev dan prod
- Port: 8080
- DB: 
	* H2 in-memory (jdbc:h2:mem:logitrackdb), ddl-auto: create-drop, untuk env local
	* Postgresql untuk env dev dan prod
- JWT secret: Base64, expiration: 10 menit (600000ms)
- JWT claims: sub (username) + roles (["ROLE_ADMIN"] / ["ROLE_USER"])
- Log4j2: rolling file harian + size, history 30 hari
- defer-datasource-initialization: true (data.sql jalan setelah DDL)

## Call external rest service, bisa call service rest gratis di internet 

## Pagination Response Format
Semua list endpoint menggunakan custom DTO (bukan Spring Page langsung):

## Logging & Tracing
- AspectJ: log semua method controller (entry + exit + duration)
- GlobalExceptionHandler: log setiap exception dengan @Slf4j
- Log4j2 XML: console + file rolling (logs/app.log), pattern: `timestamp | level | thread | logger | traceId=... | message`
- **TraceIdLoggingFilter** (`config/TraceIdLoggingFilter.java`): mengisi `traceId` dan `spanId` ke MDC secara eksplisit untuk setiap HTTP request. Log di luar request (startup, background) akan menampilkan `NO_TRACE`.
- Dependency tracing: hanya `micrometer-tracing-bridge-otel` (tanpa `opentelemetry-exporter-otlp` — hanya dibutuhkan jika ada OTel Collector)
- `management.tracing.sampling.probability=1.0` (100% request di-trace)

---

Ikuti konvensi ini secara konsisten untuk semua fitur baru.
Gunakan Lombok (@Getter, @Builder, @RequiredArgsConstructor, dll).
Semua exception dihandle di GlobalExceptionHandler.

## Sample Entity (sudah dibuat)
- `User` ↔ `Role`: Many-to-Many (join table: user_roles)
- `Category` → `Product`: One-to-Many
- `Product` ↔ `Tag`: Many-to-Many (join table: product_tags)
- Contoh native query ada di `CategoryRepository` dan `ProductRepository`

## Data Awal (profile local)
- Berjalan via `DataInitializer.java` (CommandLineRunner, @Profile("local"))
- User: `admin / admin123` (ROLE_ADMIN + ROLE_USER), `user / user123` (ROLE_USER)
- Postman collection tersedia di: `rest-template.postman_collection.json`

## Main Class
`RestTemplateApplication.java` (entry point aplikasi)
