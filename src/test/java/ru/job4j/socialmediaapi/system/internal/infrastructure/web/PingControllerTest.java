package ru.job4j.socialmediaapi.system.internal.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест веб-слоя {@link PingController}: только Spring MVC, без БД и Docker.
 */
@WebMvcTest(PingController.class)
@DisplayName("Ping: веб-слой")
class PingControllerTest {

    /** MockMvc to call the controller without a running server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/ping возвращает 200 и статус сервиса")
    void whenPingThenReturnServiceStatus() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("social-media-api"));
    }
}
