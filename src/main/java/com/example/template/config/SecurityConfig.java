package com.example.template.config;

import com.example.template.security.DynamicAuthorizationManager;
import com.example.template.security.JwtAccessDeniedHandler;
import com.example.template.security.JwtAuthenticationEntryPoint;
import com.example.template.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Konfigurasi Spring Security.
 *
 * ATURAN OTORISASI sekarang bersifat DINAMIS — dibaca dari tabel endpoint_permissions di DB
 * via DynamicAuthorizationManager, bukan hardcoded di sini.
 *
 * Untuk menambah/mengubah/menghapus aturan akses: gunakan endpoint /api/permissions (ADMIN only).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Mengaktifkan @PreAuthorize sebagai safety net (misal di EndpointPermissionController)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final DynamicAuthorizationManager dynamicAuthorizationManager;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Matikan CSRF karena kita pakai JWT stateless
                .csrf(AbstractHttpConfigurer::disable)

                // Izinkan H2 console ditampilkan dalam iframe
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                // Stateless: tidak ada session yang disimpan di server
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Konfigurasi error handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)           // 403
                )

                // Semua aturan otorisasi dikelola oleh DynamicAuthorizationManager (dari DB)
                // Untuk mengubah akses endpoint: gunakan /api/permissions (tanpa restart)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().access(dynamicAuthorizationManager)
                )

                // Pasang JWT filter sebelum UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Mencegah JwtAuthenticationFilter terdaftar dua kali:
    // sekali di Spring Security chain (addFilterBefore) dan sekali di servlet chain (karena @Component)
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
