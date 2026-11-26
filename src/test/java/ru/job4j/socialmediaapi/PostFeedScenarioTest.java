package ru.job4j.socialmediaapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.job4j.socialmediaapi.shared.security.HeaderCurrentUserProvider;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий двух модулей только через REST: подписка ({@code subscriptions}) → пост автора
 * ({@code posts}) → лента подписчика → отписка → пост пропал из ленты.
 * Текущий пользователь передаётся заголовком {@code X-User-Id}. Лежит в корневом пакете,
 * так как затрагивает оба модуля; id пользователей новые в каждом запуске — контейнер общий.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Подписка, пост и лента через REST: пост автора виден подписчику до отписки")
class PostFeedScenarioTest extends PostgreSqlIntegrationTest {

    /** Заголовок текущего пользователя. */
    private static final String USER = HeaderCurrentUserProvider.HEADER;

    /** MockMvc для вызова API без запущенного сервера. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Алиса подписывается на Бориса, видит его пост в ленте, после отписки — не видит")
    void whenSubscribedThenFeedShowsPostUntilUnsubscribed() throws Exception {
        var alice = UUID.randomUUID().toString();
        var boris = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/subscribes").header(USER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(boris)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(boris));

        var created = mockMvc.perform(post("/api/v1/posts").header(USER, boris)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Пост Бориса\",\"content\":\"Текст\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var postId = JsonPath.<String>read(created, "$.id");

        mockMvc.perform(get("/api/v1/feed").header(USER, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(hasItem(postId)));

        mockMvc.perform(delete("/api/v1/subscribes/{userId}", boris).header(USER, alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/feed").header(USER, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(not(hasItem(postId))));
    }
}
