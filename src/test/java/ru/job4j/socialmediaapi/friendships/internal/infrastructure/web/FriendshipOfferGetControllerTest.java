package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The database is shared with other tests, and some of them store offers with fixed dates
 * (e.g. {@code 2026-10-06}) that can be later than the clock of the machine running the build.
 * So the application clock of this test is shifted to {@link #FUTURE}: offers created here through
 * the API are the newest ones on any machine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("GET /api/v1/friendship-offer[/{offerId}]: заявка по id и страницы всех заявок")
class FriendshipOfferGetControllerTest {

    /** Base URL of the friendship offer endpoints. */
    private static final String BASE = "/api/v1/friendship-offer";

    /** Time the application clock of this test starts from: later than any offer of other tests. */
    private static final Instant FUTURE = Instant.parse("3000-01-01T00:00:00Z");

    /** MockMvc to call the API without a running server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Заявка по id: 200, все поля; после accept — ACCEPTED и updatedAt = acceptedAt")
    void whenGetOfferThenOkWithAllFields() throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var offerId = createOffer(fromUserId, toUserId);

        mockMvc.perform(get(BASE + "/" + offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.fromUserId").value(fromUserId.toString()))
                .andExpect(jsonPath("$.toUserId").value(toUserId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        var accepted = mockMvc.perform(post(BASE + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offerId\":\"%s\",\"actorId\":\"%s\"}".formatted(offerId, toUserId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get(BASE + "/" + offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.updatedAt").value((String) JsonPath.read(accepted, "$.acceptedAt")));
    }

    @Test
    @DisplayName("Нет заявки с таким id: 404 FRIENDSHIP_OFFER_NOT_FOUND")
    void whenOfferNotFoundThenNotFound() throws Exception {
        expectError(mockMvc.perform(get(BASE + "/" + UUID.randomUUID())), 404, "FRIENDSHIP_OFFER_NOT_FOUND");
    }

    @Test
    @DisplayName("id не UUID: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
    void whenOfferIdNotUuidThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get(BASE + "/42")), 400, "INVALID_FRIENDSHIP_OFFER_REQUEST");
    }

    @Test
    @DisplayName("Страницы: новые заявки первыми, next ведёт на следующую страницу без повторов")
    void whenPagedThenNewestFirstAndNextLeadsOn() throws Exception {
        var first = createOffer(UUID.randomUUID(), UUID.randomUUID());
        var second = createOffer(UUID.randomUUID(), UUID.randomUUID());
        var third = createOffer(UUID.randomUUID(), UUID.randomUUID());

        var page = mockMvc.perform(get(BASE).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(2))
                .andExpect(jsonPath("$.offers[0].id").value(third.toString()))
                .andExpect(jsonPath("$.offers[1].id").value(second.toString()))
                .andExpect(jsonPath("$.next.id").value(second.toString()))
                .andExpect(jsonPath("$.next.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get(BASE)
                        .param("limit", "2")
                        .param("afterCreatedAt", JsonPath.<String>read(page, "$.next.createdAt"))
                        .param("afterId", JsonPath.<String>read(page, "$.next.id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers[0].id").value(first.toString()));
    }

    @Test
    @DisplayName("Без limit страница содержит не больше 20 заявок (значение по умолчанию из контракта)")
    void whenNoLimitThenDefaultLimit() throws Exception {
        for (int i = 0; i < 21; i++) {
            createOffer(UUID.randomUUID(), UUID.randomUUID());
        }

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(20));
    }

    @Test
    @DisplayName("После последней заявки: пустая страница и next: null в теле")
    void whenNothingAfterCursorThenEmptyPageWithNullNext() throws Exception {
        var body = mockMvc.perform(get(BASE)
                        .param("afterCreatedAt", "1970-01-01T00:00:00Z")
                        .param("afterId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers").isEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"next\":null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "limit=0",
            "limit=101",
            "limit=abc",
            "afterCreatedAt=yesterday&afterId=00000000-0000-0000-0000-000000000001",
            "afterCreatedAt=2026-10-12T10:15:00Z&afterId=42",
            "afterCreatedAt=2026-10-12T10:15:00Z",
            "afterId=00000000-0000-0000-0000-000000000001"
    })
    @DisplayName("Некорректные параметры страницы: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
    void whenInvalidPageParamsThenBadRequest(String query) throws Exception {
        expectError(mockMvc.perform(get(BASE + "?" + query)), 400, "INVALID_FRIENDSHIP_OFFER_REQUEST");
    }

    /** Replaces the application clock with a ticking one shifted to {@link #FUTURE}. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FutureClockConfiguration {

        /**
         * @return clock ticking from {@link #FUTURE}: offers created one after another get growing times
         */
        @Bean
        @Primary
        Clock futureClock() {
            return Clock.offset(Clock.systemUTC(), Duration.between(Instant.now(), FUTURE));
        }
    }

    private UUID createOffer(UUID fromUserId, UUID toUserId) throws Exception {
        var json = mockMvc.perform(post(BASE + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"%s\",\"toUserId\":\"%s\"}".formatted(fromUserId, toUserId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static void expectError(ResultActions actions, int status, String code) throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
