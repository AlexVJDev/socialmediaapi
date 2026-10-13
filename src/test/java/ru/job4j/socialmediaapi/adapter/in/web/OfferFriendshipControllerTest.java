package ru.job4j.socialmediaapi.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("POST /api/offer-friendships/create: ответы и ошибки по схеме friendship_transitions.puml")
class OfferFriendshipControllerTest {

    /** URL of the create offer endpoint. */
    private static final String CREATE = "/api/offer-friendships/create";

    /** MockMvc to call the API without a running server. */
    @Autowired
    private MockMvc mockMvc;

    /** Direct access to the database to prepare and check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Без toUserId: 400 INVALID_FRIENDSHIP_OFFER_REQUEST, заявка не создаётся")
    void whenToUserIdMissingThenBadRequest() throws Exception {
        var fromUserId = UUID.randomUUID();

        expectError(
                create("{\"fromUserId\":\"%s\"}".formatted(fromUserId)),
                400, "INVALID_FRIENDSHIP_OFFER_REQUEST"
        );
        assertThat(offersFrom(fromUserId)).isZero();
        assertThat(subscriptionsOf(fromUserId)).isZero();
    }

    @Test
    @DisplayName("fromUserId не UUID: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
    void whenFromUserIdNotUuidThenBadRequest() throws Exception {
        var toUserId = UUID.randomUUID();

        expectError(
                create("{\"fromUserId\":\"42\",\"toUserId\":\"%s\"}".formatted(toUserId)),
                400, "INVALID_FRIENDSHIP_OFFER_REQUEST"
        );
        assertThat(offersTo(toUserId)).isZero();
    }

    @Test
    @DisplayName("Некорректный JSON: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
    void whenBodyNotJsonThenBadRequest() throws Exception {
        expectError(create("{not json"), 400, "INVALID_FRIENDSHIP_OFFER_REQUEST");
    }

    @Test
    @DisplayName("Заявка самому себе: 400 INVALID_FRIENDSHIP_OFFER_REQUEST, заявка и подписка не создаются")
    void whenOfferToSelfThenBadRequest() throws Exception {
        var userId = UUID.randomUUID();

        expectError(create(body(userId, userId)), 400, "INVALID_FRIENDSHIP_OFFER_REQUEST");
        assertThat(offersFrom(userId)).isZero();
        assertThat(subscriptionsOf(userId)).isZero();
    }

    @Test
    @DisplayName("Новая заявка: 201, PENDING, создаются заявка и подписка инициатора на получателя")
    void whenNewOfferThenCreated() throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();

        create(body(fromUserId, toUserId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fromUserId").value(fromUserId.toString()))
                .andExpect(jsonPath("$.toUserId").value(toUserId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        assertThat(offerStatus(fromUserId, toUserId)).isEqualTo("PENDING");
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    @Test
    @DisplayName("Повтор при PENDING: 204 без тела, дубликаты не создаются")
    void whenRepeatPendingOfferThenNoContent() throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        create(body(fromUserId, toUserId)).andExpect(status().isCreated());

        create(body(fromUserId, toUserId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(offersFrom(fromUserId)).isOne();
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    @ParameterizedTest
    @CsvSource({
            "ACCEPTED, FRIENDSHIP_OFFER_ALREADY_ACCEPTED",
            "REJECTED, FRIENDSHIP_OFFER_ALREADY_REJECTED"
    })
    @DisplayName("Повтор при ACCEPTED/REJECTED: 409 с кодом статуса, данные не меняются")
    void whenRepeatProcessedOfferThenConflict(String offerStatus, String code) throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        create(body(fromUserId, toUserId)).andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE offer_friendships SET status = ? WHERE from_user_id = ? AND to_user_id = ?",
                offerStatus, fromUserId, toUserId
        );

        expectError(create(body(fromUserId, toUserId)), 409, code);

        assertThat(offersFrom(fromUserId)).isOne();
        assertThat(offerStatus(fromUserId, toUserId)).isEqualTo(offerStatus);
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post(CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static void expectError(ResultActions actions, int status, String code) throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private static String body(UUID fromUserId, UUID toUserId) {
        return "{\"fromUserId\":\"%s\",\"toUserId\":\"%s\"}".formatted(fromUserId, toUserId);
    }

    private int offersFrom(UUID userId) {
        return count("SELECT count(*) FROM offer_friendships WHERE from_user_id = ?", userId);
    }

    private int offersTo(UUID userId) {
        return count("SELECT count(*) FROM offer_friendships WHERE to_user_id = ?", userId);
    }

    private int subscriptionsOf(UUID followerId) {
        return count("SELECT count(*) FROM subscriptions WHERE follower_id = ?", followerId);
    }

    private int subscriptions(UUID followerId, UUID followedId) {
        return count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                followerId, followedId
        );
    }

    private String offerStatus(UUID fromUserId, UUID toUserId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM offer_friendships WHERE from_user_id = ? AND to_user_id = ?",
                String.class, fromUserId, toUserId
        );
    }

    private int count(String sql, Object... args) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, args));
    }
}
