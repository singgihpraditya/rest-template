package com.example.template.dto.response;

import com.example.template.entity.EndpointPermission;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EndpointPermissionResponse {

    private Long id;
    private String httpMethod;
    private String urlPattern;
    private String requiredRole;
    private int sortOrder;
    private boolean active;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EndpointPermissionResponse from(EndpointPermission ep) {
        return EndpointPermissionResponse.builder()
                .id(ep.getId())
                .httpMethod(ep.getHttpMethod())
                .urlPattern(ep.getUrlPattern())
                .requiredRole(ep.getRequiredRole())
                .sortOrder(ep.getSortOrder())
                .active(ep.isActive())
                .description(ep.getDescription())
                .createdAt(ep.getCreatedAt())
                .updatedAt(ep.getUpdatedAt())
                .build();
    }
}
