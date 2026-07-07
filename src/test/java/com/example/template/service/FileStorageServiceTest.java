package com.example.template.service;

import com.example.template.exception.BusinessException;
import com.example.template.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        fileStorageService.init();
    }

    @Test
    void init_createsUploadDirectory() {
        assertThat(tempDir).exists();
        assertThat(ReflectionTestUtils.getField(fileStorageService, "uploadPath")).isNotNull();
    }

    @Test
    void init_ioException_throwsRuntimeException() {
        FileStorageService newService = new FileStorageService();
        ReflectionTestUtils.setField(newService, "uploadDir", tempDir.resolve("sub").toString());

        try (MockedStatic<Files> mockFiles = mockStatic(Files.class)) {
            mockFiles.when(() -> Files.createDirectories(any(Path.class)))
                    .thenThrow(new IOException("cannot create dir"));

            assertThatThrownBy(newService::init)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Gagal membuat direktori upload");
        }
    }

    @Test
    void storeFile_withExtension_returnsUniqueFilename() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-data".getBytes());

        String result = fileStorageService.storeFile(file);

        assertThat(result).endsWith(".jpg");
        assertThat(result).hasSize(40); // UUID (36) + dot (1) + ext (3)
        assertThat(Files.exists(tempDir.resolve(result))).isTrue();
    }

    @Test
    void storeFile_withoutExtension_returnsUniqueFilenameNoExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "datafile", "text/plain", "data".getBytes());

        String result = fileStorageService.storeFile(file);

        assertThat(result).doesNotContain(".");
        assertThat(result).hasSize(36); // UUID only
    }

    @Test
    void storeFile_emptyFilename_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tidak boleh kosong");
    }

    @Test
    void storeFile_pathTraversal_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../evil.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tidak valid");
    }

    @Test
    void storeFile_ioException_throwsRuntimeException() throws IOException {
        org.springframework.web.multipart.MultipartFile mockFile =
                mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getInputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> fileStorageService.storeFile(mockFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Gagal menyimpan file");
    }

    @Test
    void loadFileAsResource_success() throws IOException {
        Path testFile = tempDir.resolve("existing.txt");
        Files.writeString(testFile, "hello");

        Resource resource = fileStorageService.loadFileAsResource("existing.txt");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void loadFileAsResource_fileNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("nonexistent.txt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nonexistent.txt");
    }

    @Test
    void loadFileAsResource_existsButNotReadable_throwsResourceNotFoundException() {
        try (MockedConstruction<UrlResource> mocked = mockConstruction(UrlResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenReturn(true);
                    when(mock.isReadable()).thenReturn(false);
                })) {
            assertThatThrownBy(() -> fileStorageService.loadFileAsResource("notreadable.txt"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("notreadable.txt");
        }
    }

    @Test
    void loadFileAsResource_malformedUrl_throwsResourceNotFoundException() throws Exception {
        Path mockUploadPath = mock(Path.class);
        Path mockResolved = mock(Path.class);
        Path mockNormalized = mock(Path.class);

        // URI dengan scheme yang tidak dikenal → UrlResource.toURL() akan lempar MalformedURLException
        URI bogusUri = new URI("xyz://invalid/path");

        ReflectionTestUtils.setField(fileStorageService, "uploadPath", mockUploadPath);
        when(mockUploadPath.resolve("bad.txt")).thenReturn(mockResolved);
        when(mockResolved.normalize()).thenReturn(mockNormalized);
        when(mockNormalized.toUri()).thenReturn(bogusUri);

        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("bad.txt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("bad.txt");
    }
}
