package com.example.template.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Nama produk tidak boleh kosong")
    private String name;

    private String description;

    @NotNull(message = "Harga tidak boleh kosong")
    @DecimalMin(value = "0.0", message = "Harga tidak boleh negatif")
    private BigDecimal price;

    @Min(value = 0, message = "Stok tidak boleh negatif")
    private int stock;

    private String imageUrl;

    @NotNull(message = "ID Kategori tidak boleh kosong")
    private Long categoryId;

    // IDs tag yang akan di-assign ke produk ini
    private Set<Long> tagIds = new HashSet<>();

    // Kapan produk mulai dipublikasikan (opsional, format: "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
}
