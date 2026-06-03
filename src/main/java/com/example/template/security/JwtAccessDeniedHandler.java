package com.example.template.security;

import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.ErrorSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Menangani error 403 Forbidden.
 * Dipanggil ketika user sudah login tapi tidak punya hak akses ke resource tertentu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Forbidden access ke: {} | Pesan: {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> apiResponse = ApiResponse.error(
                ErrorSchema.forbidden(
                        "You do not have permission to access this resource",
                        "Anda tidak memiliki izin untuk mengakses resource ini"
                )
        );

        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
