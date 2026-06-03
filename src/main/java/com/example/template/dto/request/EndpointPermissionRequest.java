package com.example.template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EndpointPermissionRequest {

    @NotBlank(message = "HTTP method tidak boleh kosong")
    @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH|\\*",
             message = "HTTP method harus salah satu dari: GET, POST, PUT, DELETE, PATCH, *")
    private String httpMethod;

    @NotBlank(message = "URL pattern tidak boleh kosong")
    private String urlPattern;

    // Kosong / null = endpoint publik (tidak butuh login)
    private String requiredRole;

    @NotNull(message = "Sort order tidak boleh kosong")
    private Integer sortOrder;

    private boolean active = true;

    private String description;
}
