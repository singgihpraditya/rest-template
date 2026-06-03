package com.example.template.exception;

import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.ErrorSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Handler global untuk semua exception yang tidak ditangani di level controller.
 * Semua exception di-convert ke format ApiResponse standar.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // --- RST-001: Validation Error (400) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorDetail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errorDetail);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(ErrorSchema.validationError(
                        "Validation failed: " + errorDetail,
                        "Validasi gagal: " + errorDetail
                ))
        );
    }

    // --- RST-002: Resource Not Found (404) ---
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(ErrorSchema.notFound(
                        ex.getMessage(),
                        ex.getMessage()
                ))
        );
    }

    // --- RST-003: Business Rule Violation (409) ---
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(ErrorSchema.businessError(
                        ex.getMessage(),
                        ex.getMessage()
                ))
        );
    }

    // --- RST-401: Unauthorized (401) ---
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error(ErrorSchema.unauthorized(
                        "Authentication failed: " + ex.getMessage(),
                        "Autentikasi gagal: " + ex.getMessage()
                ))
        );
    }

    // --- RST-403: Forbidden (403) ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(ErrorSchema.forbidden(
                        "Access denied",
                        "Akses ditolak"
                ))
        );
    }

    // --- File Upload terlalu besar ---
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload terlalu besar: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(ErrorSchema.validationError(
                        "File size exceeds maximum allowed size",
                        "Ukuran file melebihi batas maksimum yang diizinkan"
                ))
        );
    }

    // --- RST-500: Internal Server Error (500) ---
    // Catch-all untuk exception yang tidak terhandle di atas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(ErrorSchema.internalError(
                        "An unexpected error occurred",
                        "Terjadi kesalahan yang tidak terduga"
                ))
        );
    }
}
