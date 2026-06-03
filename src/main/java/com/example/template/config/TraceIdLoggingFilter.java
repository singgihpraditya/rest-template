package com.example.template.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter untuk memastikan traceId dan spanId selalu terisi di log.
 *
 * KENAPA DIBUTUHKAN?
 * Spring Boot auto-config untuk MDC (Slf4jCorrelationScopeDecorator) bekerja baik
 * dengan Logback, tapi dengan Log4j2 kadang tidak terpropagasi otomatis.
 * Filter ini mengambil traceId dari Micrometer Tracer dan memasukkannya ke MDC secara eksplisit.
 *
 * URUTAN EKSEKUSI:
 * 1. Micrometer ObservationFilter (HIGHEST_PRECEDENCE + 1) → membuat span baru
 * 2. Filter ini         (HIGHEST_PRECEDENCE + 5) → mengambil traceId dari span → isi MDC
 * 3. Spring Security Filter (~-100)              → autentikasi JWT
 * 4. Controller / Service                        → semua log sudah punya traceId
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TraceIdLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID  = "spanId";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Ambil span yang sudah dibuat oleh Micrometer ObservationFilter
        Span currentSpan = tracer.currentSpan();

        boolean mdcSet = false;
        if (currentSpan != null && !currentSpan.isNoop()) {
            MDC.put(TRACE_ID, currentSpan.context().traceId());
            MDC.put(SPAN_ID,  currentSpan.context().spanId());
            mdcSet = true;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Selalu bersihkan MDC setelah request selesai
            if (mdcSet) {
                MDC.remove(TRACE_ID);
                MDC.remove(SPAN_ID);
            }
        }
    }
}
