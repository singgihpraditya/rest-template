package com.example.template.controller;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticControllerTest {

    @Mock
    private Tracer tracer;

    @InjectMocks
    private DiagnosticController diagnosticController;

    @Test
    void checkTrace_spanActiveAndNotNoop_returnsOkWithTraceInfo() {
        Span mockSpan = mock(Span.class);
        TraceContext mockContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(mockSpan);
        when(mockSpan.isNoop()).thenReturn(false);
        when(mockSpan.context()).thenReturn(mockContext);
        when(mockContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
        when(mockContext.spanId()).thenReturn("00f067aa0ba902b7");

        ResponseEntity<Map<String, Object>> response = diagnosticController.checkTrace();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("OK");
        assertThat(response.getBody().get("trace_id")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(response.getBody().get("span_id")).isEqualTo("00f067aa0ba902b7");
        assertThat(response.getBody()).containsKey("mdc_trace_id");
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void checkTrace_spanIsNull_returnsNotActive() {
        when(tracer.currentSpan()).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = diagnosticController.checkTrace();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("NOT_ACTIVE");
        assertThat(response.getBody().get("trace_id")).isNull();
        assertThat(response.getBody().get("span_id")).isNull();
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void checkTrace_spanIsNoop_returnsNotActive() {
        Span mockSpan = mock(Span.class);
        when(tracer.currentSpan()).thenReturn(mockSpan);
        when(mockSpan.isNoop()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = diagnosticController.checkTrace();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("NOT_ACTIVE");
    }
}
