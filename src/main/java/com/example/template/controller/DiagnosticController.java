package com.example.template.controller;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint untuk memverifikasi bahwa OpenTelemetry / distributed tracing berjalan.
 *
 * Cara pakai:
 *   1. Buka GET /api/diagnostic/trace
 *   2. Pastikan "trace_id" dan "span_id" terisi (bukan null atau empty)
 *   3. Cek log — baris log dari request ini harus menampilkan traceId yang sama
 */
@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Tag(name = "Diagnostic", description = "Endpoint untuk verifikasi OpenTelemetry tracing")
public class DiagnosticController {

    private final Tracer tracer;

    @GetMapping("/trace")
    @Operation(
        summary = "Cek status tracing",
        description = """
            Verifikasi bahwa OpenTelemetry tracing berjalan dengan benar.

            Response akan berisi:
            - trace_id: ID trace aktif (harus berisi hex string 32 karakter)
            - span_id:  ID span aktif (harus berisi hex string 16 karakter)
            - mdc_trace_id: traceId dari MDC (harus sama dengan trace_id)
            - status: 'OK' jika tracing aktif, 'NOT_ACTIVE' jika tidak

            Setelah hit endpoint ini, cek log — baris log harus mengandung
            traceId= yang sama dengan nilai trace_id di response.
            """
    )
    public ResponseEntity<Map<String, Object>> checkTrace() {
        Map<String, Object> result = new LinkedHashMap<>();

        Span currentSpan = tracer.currentSpan();

        if (currentSpan != null && !currentSpan.isNoop()) {
            String traceId = currentSpan.context().traceId();
            String spanId  = currentSpan.context().spanId();

            result.put("status",       "OK");
            result.put("trace_id",     traceId);
            result.put("span_id",      spanId);
            result.put("mdc_trace_id", MDC.get("traceId"));  // harus sama dengan trace_id
            result.put("message",      "Tracing aktif. Cek log untuk baris dengan traceId=" + traceId);
        } else {
            result.put("status",       "NOT_ACTIVE");
            result.put("trace_id",     null);
            result.put("span_id",      null);
            result.put("mdc_trace_id", MDC.get("traceId"));
            result.put("message",      "Span tidak aktif. Pastikan micrometer-tracing-bridge-otel ada di classpath.");
        }

        return ResponseEntity.ok(result);
    }
}
