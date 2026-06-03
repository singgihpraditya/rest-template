package com.example.template.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO untuk response dari JSONPlaceholder API (https://jsonplaceholder.typicode.com/posts).
 * Menggunakan @JsonProperty karena API eksternal menggunakan camelCase (bukan snake_case).
 */
@Getter
@Setter
@NoArgsConstructor
public class PostResponse {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("userId")
    private Integer userId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;
}
