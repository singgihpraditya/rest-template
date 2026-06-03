package com.example.template.service;

import com.example.template.exception.BusinessException;
import com.example.template.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    // Buat direktori upload saat aplikasi start
    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory siap: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Gagal membuat direktori upload: " + uploadPath, e);
        }
    }

    /**
     * Simpan file ke direktori upload.
     * Nama file di-generate secara unik (UUID) untuk mencegah konflik.
     *
     * @return nama file yang tersimpan
     */
    public String storeFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFilename.isEmpty()) {
            throw new BusinessException("Nama file tidak boleh kosong");
        }
        if (originalFilename.contains("..")) {
            throw new BusinessException("Nama file tidak valid: " + originalFilename);
        }

        // Generate nama unik: uuid + ekstensi original
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File berhasil disimpan: {}", uniqueFilename);
            return uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan file: " + originalFilename, e);
        }
    }

    /**
     * Load file sebagai Resource untuk di-download/di-serve.
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File tidak ditemukan: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File tidak ditemukan: " + filename);
        }
    }
}
