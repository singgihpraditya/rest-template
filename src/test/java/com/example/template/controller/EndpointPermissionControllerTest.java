package com.example.template.controller;

import com.example.template.dto.request.EndpointPermissionRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.EndpointPermissionResponse;
import com.example.template.service.EndpointPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointPermissionControllerTest {

    @Mock
    private EndpointPermissionService permissionService;

    @InjectMocks
    private EndpointPermissionController permissionController;

    private EndpointPermissionResponse buildResponse(Long id, String method, String pattern) {
        return EndpointPermissionResponse.builder()
                .id(id).httpMethod(method).urlPattern(pattern)
                .requiredRole("ROLE_USER").sortOrder(10).active(true)
                .description("Test").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private EndpointPermissionRequest buildRequest(String method, String pattern) {
        EndpointPermissionRequest req = new EndpointPermissionRequest();
        ReflectionTestUtils.setField(req, "httpMethod", method);
        ReflectionTestUtils.setField(req, "urlPattern", pattern);
        ReflectionTestUtils.setField(req, "requiredRole", "ROLE_USER");
        ReflectionTestUtils.setField(req, "sortOrder", 50);
        ReflectionTestUtils.setField(req, "active", true);
        ReflectionTestUtils.setField(req, "description", "Test");
        return req;
    }

    @Test
    void getAll_returnsOkWithPermissionList() {
        EndpointPermissionResponse response = buildResponse(1L, "GET", "/api/**");
        when(permissionService.findAll()).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<List<EndpointPermissionResponse>>> result =
                permissionController.getAll();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getOutputSchema()).hasSize(1);
        assertThat(result.getBody().getOutputSchema().get(0).getHttpMethod()).isEqualTo("GET");
    }

    @Test
    void getById_returnsOkWithPermission() {
        EndpointPermissionResponse response = buildResponse(1L, "GET", "/api/**");
        when(permissionService.findById(1L)).thenReturn(response);

        ResponseEntity<ApiResponse<EndpointPermissionResponse>> result =
                permissionController.getById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getOutputSchema().getId()).isEqualTo(1L);
    }

    @Test
    void create_returnsCreatedWithPermissionResponse() {
        EndpointPermissionRequest request = buildRequest("GET", "/api/orders/**");
        EndpointPermissionResponse response = buildResponse(1L, "GET", "/api/orders/**");
        when(permissionService.create(request)).thenReturn(response);

        ResponseEntity<ApiResponse<EndpointPermissionResponse>> result =
                permissionController.create(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getOutputSchema().getUrlPattern()).isEqualTo("/api/orders/**");
    }

    @Test
    void update_returnsOkWithUpdatedPermission() {
        EndpointPermissionRequest request = buildRequest("POST", "/api/orders/**");
        EndpointPermissionResponse response = buildResponse(1L, "POST", "/api/orders/**");
        when(permissionService.update(1L, request)).thenReturn(response);

        ResponseEntity<ApiResponse<EndpointPermissionResponse>> result =
                permissionController.update(1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getOutputSchema().getHttpMethod()).isEqualTo("POST");
    }

    @Test
    void delete_returnsOkWithNullData() {
        ResponseEntity<ApiResponse<Void>> result =
                permissionController.delete(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getOutputSchema()).isNull();
        verify(permissionService).delete(1L);
    }

    @Test
    void refresh_returnsOkAndCallsService() {
        ResponseEntity<ApiResponse<Void>> result =
                permissionController.refresh();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getOutputSchema()).isNull();
        verify(permissionService).refresh();
    }
}
