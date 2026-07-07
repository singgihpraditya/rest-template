package com.example.template.controller;

import com.example.template.dto.response.ApiResponse;
import com.example.template.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8080);
        mockRequest.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void uploadFile_returnsOkWithFileInfo() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-content".getBytes());
        when(fileStorageService.storeFile(any())).thenReturn("uuid-photo.jpg");

        ResponseEntity<ApiResponse<Map<String, String>>> response =
                fileController.uploadFile(mockFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, String> body = response.getBody().getOutputSchema();
        assertThat(body.get("filename")).isEqualTo("uuid-photo.jpg");
        assertThat(body.get("file_url")).contains("uuid-photo.jpg");
        assertThat(body.get("original_name")).isEqualTo("photo.jpg");
        assertThat(body.get("size")).isEqualTo("12");
    }

    @Test
    void downloadFile_returnsOkWithResourceAndContentDisposition() {
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFilename()).thenReturn("photo.jpg");
        when(fileStorageService.loadFileAsResource("photo.jpg")).thenReturn(mockResource);

        ResponseEntity<Resource> response = fileController.downloadFile("photo.jpg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"photo.jpg\"");
        assertThat(response.getBody()).isEqualTo(mockResource);
    }
}
