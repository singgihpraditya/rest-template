package com.example.template.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Konfigurasi Web MVC.
 * Mendaftarkan direktori upload sebagai static resource
 * sehingga file yang di-upload bisa diakses melalui URL.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir);
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();

        // File yang di-upload bisa diakses via /files/nama-file.jpg
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/");
    }
}
