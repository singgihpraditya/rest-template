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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Login: autentikasi user dan generate JWT token.
     */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", null));

        log.info("User berhasil login: {}", request.getUsername());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(UserResponse.from(user))
                .build();
    }

    /**
     * Register: daftarkan user baru dengan role ROLE_USER.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' sudah digunakan");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' sudah terdaftar");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_USER tidak ditemukan"));

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .active(true)
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("User baru terdaftar: {}", savedUser.getUsername());

        return UserResponse.from(savedUser);
    }

    /**
     * Ambil data user yang sedang login berdasarkan username dari JWT.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan: " + username));
        return UserResponse.from(user);
    }
}
