package com.example.demo.sentiment_analysis.checker;

import com.example.demo.sentiment_analysis.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthChecker.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthCheckerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/health_checker → 200 OK with body 'Ok fine'")
    void healthChecker_returns200WithBody() throws Exception {
        mockMvc.perform(get("/api/health_checker"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ok fine"));
    }
}
