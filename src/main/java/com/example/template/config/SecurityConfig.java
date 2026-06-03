package com.example.template.config;

import com.example.template.security.JwtAccessDeniedHandler;
import com.example.template.security.JwtAuthenticationEntryPoint;
import com.example.template.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * Menggunakan JWT stateless (tidak ada session).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Mengaktifkan @PreAuthorize, @PostAuthorize di level method
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // URL yang boleh diakses tanpa token
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/h2-console/**",
            "/actuator/health"
    };

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

                // Aturan otorisasi per endpoint
                .authorizeHttpRequests(auth -> auth
                        // URL publik
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // GET endpoints yang boleh diakses tanpa login
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/external/**"   // demo feign client, data publik dari JSONPlaceholder
                        ).permitAll()
                        // Semua request lainnya harus authenticated
                        .anyRequest().authenticated()
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

    /**
     * Mencegah Spring Boot mendaftarkan JwtAuthenticationFilter secara otomatis
     * ke servlet filter chain (karena @Component).
     *
     * KENAPA DIBUTUHKAN?
     * JwtAuthenticationFilter adalah @Component, sehingga Spring Boot secara otomatis
     * mendaftarkannya ke servlet filter chain DAN kita juga mendaftarkannya secara eksplisit
     * ke Spring Security chain via addFilterBefore(). Tanpa ini, filter berjalan dua kali:
     *   1. Di servlet chain (redundant, sebelum Security chain selesai)
     *   2. Di Spring Security chain (yang benar, di dalam SecurityFilterChain)
     *
     * Dengan setEnabled(false), filter HANYA berjalan di dalam Spring Security chain.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
