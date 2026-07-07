package com.example.template.controller;

import com.example.template.dto.request.LoginRequest;
import com.example.template.dto.request.RegisterRequest;
import com.example.template.dto.response.ApiResponse;
import com.example.template.dto.response.TokenResponse;
import com.example.template.dto.response.UserResponse;
import com.example.template.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserResponse buildUserResponse(String username) {
        return UserResponse.builder()
                .id(1L).username(username).email(username + "@test.com")
                .fullName("Test User").active(true)
                .roles(Set.of("ROLE_USER")).build();
    }

    @Test
    void login_returnsOkWithToken() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "username", "admin");
        ReflectionTestUtils.setField(request, "password", "admin123");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("jwt-token").tokenType("Bearer").expiresIn(600000L)
                .user(buildUserResponse("admin")).build();
        when(authService.login(request)).thenReturn(tokenResponse);

        ResponseEntity<ApiResponse<TokenResponse>> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOutputSchema().getAccessToken()).isEqualTo("jwt-token");
    }

    @Test
    void register_returnsCreatedWithUserResponse() {
        RegisterRequest request = new RegisterRequest();
        ReflectionTestUtils.setField(request, "username", "newuser");
        ReflectionTestUtils.setField(request, "password", "pass123");
        ReflectionTestUtils.setField(request, "email", "new@test.com");

        UserResponse userResponse = buildUserResponse("newuser");
        when(authService.register(request)).thenReturn(userResponse);

        ResponseEntity<ApiResponse<UserResponse>> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOutputSchema().getUsername()).isEqualTo("newuser");
    }

    @Test
    void getCurrentUser_returnsOkWithCurrentUser() {
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("admin");

        UserResponse userResponse = buildUserResponse("admin");
        when(authService.getCurrentUser("admin")).thenReturn(userResponse);

        ResponseEntity<ApiResponse<UserResponse>> response =
                authController.getCurrentUser(mockUserDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOutputSchema().getUsername()).isEqualTo("admin");
    }
}
