package ru.job4j.socialmediaapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/ping} в полном контексте приложения: JPA и Liquibase на PostgreSQL из
 * {@link PostgreSqlIntegrationTest}, запрос через Spring MVC без отдельного HTTP-сервера.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Ping: полный контекст с PostgreSQL")
class PingControllerIntegrationTest extends PostgreSqlIntegrationTest {

    /** MockMvc to call the API without a running server. */
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
