package com.example.template.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Konfigurasi Jackson secara global.
 *
 * Dengan config ini, SEMUA request dan response otomatis menggunakan snake_case —
 * tidak perlu @JsonNaming di setiap class DTO.
 *
 * Contoh konversi otomatis:
 *   Java field  → JSON (response)   | JSON (request) → Java field
 *   categoryId  → "category_id"     | "category_id"  → categoryId
 *   fullName    → "full_name"       | "full_name"    → fullName
 *   imageUrl    → "image_url"       | "image_url"    → imageUrl
 *
 * Format LocalDateTime: "yyyy-MM-dd HH:mm:ss" (lebih human-readable dari ISO-8601)
 *   Sebelum: "2024-01-15T10:30:00.123456"
 *   Sesudah: "2024-01-15 10:30:00"
 *
 * PENGECUALIAN:
 * Field yang menggunakan @JsonProperty tetap mengikuti nama yang didefinisikan
 * secara eksplisit (contoh: PostResponse yang deserialize dari API eksternal).
 */
@Configuration
public class JacksonConfig {

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        // Konfigurasi JavaTimeModule dengan format LocalDateTime yang lebih readable
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME_FORMATTER));

        return builder -> builder
                // Semua field Java (camelCase) otomatis dikonversi ke/dari snake_case di JSON
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                // Gunakan modul JSR-310 dengan formatter custom (bukan ISO-8601 default)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(javaTimeModule);
    }
}
