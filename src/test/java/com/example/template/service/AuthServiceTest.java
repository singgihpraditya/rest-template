package com.example.template.service;

import com.example.template.dto.request.LoginRequest;
import com.example.template.dto.request.RegisterRequest;
import com.example.template.dto.response.TokenResponse;
import com.example.template.dto.response.UserResponse;
import com.example.template.entity.Role;
import com.example.template.entity.User;
import com.example.template.exception.BusinessException;
import com.example.template.exception.ResourceNotFoundException;
import com.example.template.repository.RoleRepository;
import com.example.template.repository.UserRepository;
import com.example.template.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 600000L);
    }

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "username", username);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }

    private RegisterRequest buildRegisterRequest(String username, String password, String email, String fullName) {
        RegisterRequest req = new RegisterRequest();
        ReflectionTestUtils.setField(req, "username", username);
        ReflectionTestUtils.setField(req, "password", password);
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "fullName", fullName);
        return req;
    }

    private User buildUser(Long id, String username, String email) {
        Role role = Role.builder().id(1L).name("ROLE_USER").build();
        return User.builder()
                .id(id).username(username).email(email)
                .password("encoded").fullName("Test User")
                .active(true).roles(Set.of(role)).build();
    }

    @Test
    void login_success() {
        LoginRequest request = buildLoginRequest("admin", "admin123");

        UserDetails mockUserDetails = mock(UserDetails.class);
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockUserDetails)).thenReturn("jwt-token");

        User user = buildUser(1L, "admin", "admin@test.com");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        TokenResponse result = authService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("jwt-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(600000L);
        assertThat(result.getUser().getUsername()).isEqualTo("admin");
        verify(authenticationManager).authenticate(any());
        verify(jwtTokenProvider).generateToken(mockUserDetails);
    }

    @Test
    void register_success() {
        RegisterRequest request = buildRegisterRequest("newuser", "pass123", "new@test.com", "New User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);

        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");

        User savedUser = buildUser(2L, "newuser", "new@test.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse result = authService.register(request);

        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("pass123");
    }

    @Test
    void register_usernameAlreadyExists_throwsBusinessException() {
        RegisterRequest request = buildRegisterRequest("admin", "pass123", "other@test.com", "Admin");
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("admin");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailAlreadyExists_throwsBusinessException() {
        RegisterRequest request = buildRegisterRequest("newuser", "pass123", "admin@test.com", "New User");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("admin@test.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_roleNotFound_throwsResourceNotFoundException() {
        RegisterRequest request = buildRegisterRequest("newuser", "pass123", "new@test.com", "New User");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getCurrentUser_success() {
        User user = buildUser(1L, "admin", "admin@test.com");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserResponse result = authService.getCurrentUser("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    void login_userNotFoundAfterAuthentication_throwsResourceNotFoundException() {
        LoginRequest request = buildLoginRequest("ghost", "pass");
        UserDetails mockUserDetails = mock(UserDetails.class);
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockUserDetails)).thenReturn("jwt-token");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCurrentUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
