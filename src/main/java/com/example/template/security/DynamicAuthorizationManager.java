package com.example.template.security;

import com.example.template.entity.EndpointPermission;
import com.example.template.service.EndpointPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * AuthorizationManager dinamis: membaca aturan akses dari DB via EndpointPermissionService.
 *
 * ALUR PENGECEKAN untuk setiap request:
 *
 *   1. Cari rule yang cocok dengan [HTTP Method + URL Path] dari cache.
 *   2. Jika rule TIDAK ditemukan → fallback: wajib login.
 *   3. Jika rule ditemukan dan requiredRole KOSONG → endpoint publik, izinkan.
 *   4. Jika requiredRole ada → cek apakah user login & punya role tersebut.
 *
 * Menggantikan aturan hardcoded di SecurityConfig, sehingga akses bisa diubah
 * lewat database tanpa restart aplikasi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final EndpointPermissionService permissionService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authSupplier,
                                       RequestAuthorizationContext context) {

        String method = context.getRequest().getMethod();
        String path   = context.getRequest().getRequestURI();

        Optional<EndpointPermission> ruleOpt = permissionService.findMatchingPermission(method, path);

        // --- Tidak ada rule yang cocok → fallback: wajib authenticated ---
        if (ruleOpt.isEmpty()) {
            log.debug("No permission rule for [{} {}], requiring authentication", method, path);
            return new AuthorizationDecision(isAuthenticated(authSupplier));
        }

        EndpointPermission rule = ruleOpt.get();

        // --- Rule ditemukan, requiredRole kosong → endpoint publik ---
        if (!StringUtils.hasText(rule.getRequiredRole())) {
            log.debug("Public access granted for [{} {}]", method, path);
            return new AuthorizationDecision(true);
        }

        // --- Rule ditemukan, ada requiredRole → cek role user ---
        if (!isAuthenticated(authSupplier)) {
            // User belum login → return false, Spring Security akan memanggil AuthenticationEntryPoint (401)
            log.debug("Unauthenticated access denied for [{} {}]", method, path);
            return new AuthorizationDecision(false);
        }

        Authentication auth = authSupplier.get();
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(rule.getRequiredRole()));

        log.debug("Access for [{} {}] requires [{}]: {}",
                method, path, rule.getRequiredRole(), hasRole ? "GRANTED" : "DENIED");

        return new AuthorizationDecision(hasRole);
    }

    private boolean isAuthenticated(Supplier<Authentication> authSupplier) {
        Authentication auth = authSupplier.get();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }
}
