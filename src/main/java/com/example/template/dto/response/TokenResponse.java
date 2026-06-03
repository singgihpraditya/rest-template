package com.example.template.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response setelah login berhasil: berisi JWT token dan info user.
 */
@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;      // dalam milidetik
    private UserResponse user;
}
