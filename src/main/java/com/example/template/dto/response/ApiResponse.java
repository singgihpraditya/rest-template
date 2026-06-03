package com.example.template.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Envelope response standar untuk semua endpoint.
 *
 * Format JSON:
 * {
 *   "error_schema": { "error_code": "RST-000", "error_message": {...} },
 *   "output_schema": { ... }
 * }
 *
 * @param <T> Tipe data payload yang dikembalikan
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // field null tidak dimasukkan ke JSON
public class ApiResponse<T> {

    private ErrorSchema errorSchema;
    private T outputSchema;

    // --- Factory methods untuk kemudahan pembuatan response ---

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .errorSchema(ErrorSchema.success())
                .outputSchema(data)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorSchema errorSchema) {
        return ApiResponse.<T>builder()
                .errorSchema(errorSchema)
                .build();
    }
}
