package com.example.template;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class RestTemplateApplicationTests {

    @Test
    void contextLoads() {
        // Verifikasi bahwa Spring Application Context berhasil start
    }
}
