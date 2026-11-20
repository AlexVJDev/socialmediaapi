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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Страницы читаются только для текущего пользователя, и у каждого теста свои случайные пользователи, поэтому
 * заявки других тестов на страницу не попадают. Часы приложения в этом тесте не зависят от системного времени:
 * каждое чтение на секунду позже предыдущего, поэтому заявки, созданные одна за другой,
 * всегда получают растущие и различные {@code created_at} даже на машине с грубыми или
 * немонотонными системными часами (CI-контейнеры).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("GET /api/v1/friendship-offer[/{offerId}]: заявка по id и страницы заявок текущего пользователя")
class FriendshipOfferGetControllerTest {

    /** Базовый URL эндпоинтов заявок в друзья. */
    private static final String BASE = "/api/v1/friendship-offer";

    /** Заголовок текущего пользователя. */
    private static final String USER_HEADER = "X-User-Id";

    /** Время первого чтения часов приложения. */
    private static final Instant CLOCK_START = Instant.parse("2026-10-01T10:00:00Z");

    /** MockMvc для вызова API без запущенного сервера. */
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
                        .header(USER_HEADER, toUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offerId\":\"%s\"}".formatted(offerId)))
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
    @DisplayName("id не UUID: 400 VALIDATION_ERROR")
    void whenOfferIdNotUuidThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get(BASE + "/42")), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("INCOMING: страницы входящих текущего пользователя, новые первыми, next ведёт на следующую страницу")
    void whenIncomingPagedThenNewestFirstAndNextLeadsOn() throws Exception {
        var user = UUID.randomUUID();
        var first = createOffer(UUID.randomUUID(), user);
        var second = createOffer(UUID.randomUUID(), user);
        var third = createOffer(UUID.randomUUID(), user);
        createOffer(user, UUID.randomUUID());

        var page = mockMvc.perform(get(BASE)
                        .header(USER_HEADER, user)
                        .param("direction", "INCOMING")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(2))
                .andExpect(jsonPath("$.offers[0].id").value(third.toString()))
                .andExpect(jsonPath("$.offers[1].id").value(second.toString()))
                .andExpect(jsonPath("$.next.id").value(second.toString()))
                .andExpect(jsonPath("$.next.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get(BASE)
                        .header(USER_HEADER, user)
                        .param("direction", "INCOMING")
                        .param("limit", "2")
                        .param("afterCreatedAt", JsonPath.<String>read(page, "$.next.createdAt"))
                        .param("afterId", JsonPath.<String>read(page, "$.next.id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(1))
                .andExpect(jsonPath("$.offers[0].id").value(first.toString()))
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    @DisplayName("OUTGOING + status: принятая исходящая — только при status=ACCEPTED; по умолчанию PENDING")
    void whenOutgoingWithStatusThenOnlyOffersInStatus() throws Exception {
        var user = UUID.randomUUID();
        var recipient = UUID.randomUUID();
        var accepted = createOffer(user, recipient);
        mockMvc.perform(post(BASE + "/accept")
                        .header(USER_HEADER, recipient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offerId\":\"%s\"}".formatted(accepted)))
                .andExpect(status().isOk());
        var pending = createOffer(user, UUID.randomUUID());

        mockMvc.perform(get(BASE).header(USER_HEADER, user).param("direction", "OUTGOING").param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(1))
                .andExpect(jsonPath("$.offers[0].id").value(accepted.toString()))
                .andExpect(jsonPath("$.offers[0].status").value("ACCEPTED"));
        mockMvc.perform(get(BASE).header(USER_HEADER, user).param("direction", "OUTGOING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(1))
                .andExpect(jsonPath("$.offers[0].id").value(pending.toString()));
    }

    @Test
    @DisplayName("Без limit страница содержит 20 заявок (значение по умолчанию из контракта)")
    void whenNoLimitThenDefaultLimit() throws Exception {
        var user = UUID.randomUUID();
        for (int i = 0; i < 21; i++) {
            createOffer(UUID.randomUUID(), user);
        }

        mockMvc.perform(get(BASE).header(USER_HEADER, user).param("direction", "INCOMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(20))
                .andExpect(jsonPath("$.next").isNotEmpty());
    }

    @Test
    @DisplayName("Нет заявок: пустая страница и next: null в теле")
    void whenNoOffersThenEmptyPageWithNullNext() throws Exception {
        var body = mockMvc.perform(get(BASE).header(USER_HEADER, UUID.randomUUID()).param("direction", "OUTGOING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers").isEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"next\":null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "direction=FOO",
            "direction=incoming",
            "direction=INCOMING&status=FOO",
            "direction=INCOMING&limit=0",
            "direction=INCOMING&limit=101",
            "direction=INCOMING&limit=abc",
            "direction=INCOMING&afterCreatedAt=yesterday&afterId=00000000-0000-0000-0000-000000000001",
            "direction=INCOMING&afterCreatedAt=2026-10-12T10:15:00Z&afterId=42",
            "direction=INCOMING&afterCreatedAt=2026-10-12T10:15:00Z",
            "direction=INCOMING&afterId=00000000-0000-0000-0000-000000000001"
    })
    @DisplayName("Нет direction или некорректные параметры: 400 VALIDATION_ERROR")
    void whenInvalidPageParamsThenBadRequest(String query) throws Exception {
        expectError(mockMvc.perform(get(BASE + "?" + query).header(USER_HEADER, UUID.randomUUID())),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Без X-User-Id или X-User-Id не UUID: 401 UNAUTHORIZED")
    void whenNoCurrentUserThenUnauthorized() throws Exception {
        expectError(mockMvc.perform(get(BASE).param("direction", "INCOMING")), 401, "UNAUTHORIZED");
        expectError(mockMvc.perform(get(BASE).header(USER_HEADER, "abc").param("direction", "INCOMING")),
                401, "UNAUTHORIZED");
    }

    /** Заменяет часы приложения на {@link SteppingClock}, начинающиеся с {@link #CLOCK_START}. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FutureClockConfiguration {

        /**
         * @return часы, начинающиеся с {@link #CLOCK_START}: заявки, созданные одна за другой, получают растущее время
         */
        @Bean
        @Primary
        Clock futureClock() {
            return new SteppingClock(CLOCK_START);
        }
    }

    /** Часы UTC, не зависящие от системного времени: каждый {@link #instant()} на секунду позже предыдущего. */
    static final class SteppingClock extends Clock {

        /** Шаг между двумя чтениями. */
        private static final Duration STEP = Duration.ofSeconds(1);

        /** Время первого чтения. */
        private final Instant start;

        /** Число уже сделанных чтений. */
        private final AtomicLong ticks = new AtomicLong();

        /**
         * @param start время первого чтения
         */
        SteppingClock(Instant start) {
            this.start = start;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("SteppingClock is UTC only");
        }

        @Override
        public Instant instant() {
            return start.plus(STEP.multipliedBy(ticks.getAndIncrement()));
        }
    }

    private UUID createOffer(UUID fromUserId, UUID toUserId) throws Exception {
        var json = mockMvc.perform(post(BASE + "/create")
                        .header(USER_HEADER, fromUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":\"%s\"}".formatted(toUserId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static void expectError(ResultActions actions, int status, String code) throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
