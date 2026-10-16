package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("POST /api/v1/friendship-offer/{create,accept,reject}: ответы и ошибки по friendship_transitions.puml")
class FriendshipOfferControllerTest {

    /** Base URL of the friendship offer endpoints. */
    private static final String BASE = "/api/v1/friendship-offer";

    /** URL of the create offer endpoint. */
    private static final String CREATE = BASE + "/create";

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
    @DisplayName("Повтор заявки в статусе PENDING: 200 FRIENDSHIP_OFFER_ALREADY_PENDING, данные не меняются")
    void whenRepeatPendingOfferThenOkWithPendingCode() throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        create(body(fromUserId, toUserId)).andExpect(status().isCreated());

        expectError(create(body(fromUserId, toUserId)), 200, "FRIENDSHIP_OFFER_ALREADY_PENDING");

        assertThat(offersFrom(fromUserId)).isOne();
        assertThat(offerStatus(fromUserId, toUserId)).isEqualTo("PENDING");
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    @Test
    @DisplayName("Встречная заявка при PENDING: 200 FRIENDSHIP_OFFER_ALREADY_PENDING, "
            + "заявка пары одна, подписки получателя на инициатора нет")
    void whenCounterOfferPendingThenOkWithPendingCode() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        create(body(aliceId, borisId)).andExpect(status().isCreated());

        expectError(create(body(borisId, aliceId)), 200, "FRIENDSHIP_OFFER_ALREADY_PENDING");

        assertThat(offersFrom(aliceId)).isOne();
        assertThat(offersFrom(borisId)).isZero();
        assertThat(offerStatus(aliceId, borisId)).isEqualTo("PENDING");
        assertThat(subscriptions(aliceId, borisId)).isOne();
        assertThat(subscriptions(borisId, aliceId)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "ACCEPTED, FRIENDSHIP_OFFER_ALREADY_ACCEPTED",
            "REJECTED, FRIENDSHIP_OFFER_ALREADY_REJECTED"
    })
    @DisplayName("Встречная заявка при ACCEPTED/REJECTED: 409 с кодом статуса заявки пары, данные не меняются")
    void whenCounterOfferDecidedThenConflict(String offerStatus, String code) throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        create(body(aliceId, borisId)).andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE friendship_offer SET status = ? WHERE from_user_id = ? AND to_user_id = ?",
                offerStatus, aliceId, borisId
        );

        expectError(create(body(borisId, aliceId)), 409, code);

        assertThat(offersFrom(borisId)).isZero();
        assertThat(offerStatus(aliceId, borisId)).isEqualTo(offerStatus);
        assertThat(subscriptions(borisId, aliceId)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "ACCEPTED, FRIENDSHIP_OFFER_ALREADY_ACCEPTED",
            "REJECTED, FRIENDSHIP_OFFER_ALREADY_REJECTED"
    })
    @DisplayName("Повтор ACCEPTED/REJECTED: 409 с кодом статуса существующей заявки, данные не меняются")
    void whenRepeatOfferThenConflict(String offerStatus, String code) throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        create(body(fromUserId, toUserId)).andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE friendship_offer SET status = ? WHERE from_user_id = ? AND to_user_id = ?",
                offerStatus, fromUserId, toUserId
        );

        expectError(create(body(fromUserId, toUserId)), 409, code);

        assertThat(offersFrom(fromUserId)).isOne();
        assertThat(offerStatus(fromUserId, toUserId)).isEqualTo(offerStatus);
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    @Test
    @DisplayName("accept PENDING получателем: 200, ACCEPTED, дружба в каноническом порядке и взаимные подписки")
    void whenAcceptPendingThenOkFriendshipAndMutualSubscriptions() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);

        decide("accept", decideBody(offerId, borisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.fromUserId").value(aliceId.toString()))
                .andExpect(jsonPath("$.toUserId").value(borisId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty())
                .andExpect(jsonPath("$.rejectedAt").doesNotExist());

        assertThat(offerStatus(aliceId, borisId)).isEqualTo("ACCEPTED");
        assertThat(friendshipsOf(offerId)).isOne();
        assertThat(count("""
                SELECT count(*) FROM friendships
                WHERE friendship_offer_id = ? AND status = 'ACTIVE' AND first_user_id < second_user_id
                  AND ((first_user_id = ? AND second_user_id = ?) OR (first_user_id = ? AND second_user_id = ?))
                """, offerId, aliceId, borisId, borisId, aliceId)).isOne();
        assertThat(subscriptions(aliceId, borisId)).isOne();
        assertThat(subscriptions(borisId, aliceId)).isOne();
    }

    @Test
    @DisplayName("reject PENDING получателем: 200, REJECTED; дружбы нет, подписка отправителя осталась, обратной нет")
    void whenRejectPendingThenOkAndNoFriendship() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);

        decide("reject", decideBody(offerId, borisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedAt").isNotEmpty())
                .andExpect(jsonPath("$.acceptedAt").doesNotExist());

        assertThat(offerStatus(aliceId, borisId)).isEqualTo("REJECTED");
        assertThat(friendshipsOf(offerId)).isZero();
        assertThat(subscriptions(aliceId, borisId)).isOne();
        assertThat(subscriptions(borisId, aliceId)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"accept, sender", "accept, stranger", "reject, sender", "reject, stranger"})
    @DisplayName("accept/reject не получателем: 404 FRIENDSHIP_OFFER_NOT_FOUND, данные не меняются")
    void whenDecideByNotRecipientThenNotFound(String action, String actor) throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);
        var actorId = "sender".equals(actor) ? aliceId : UUID.randomUUID();

        expectError(decide(action, decideBody(offerId, actorId)), 404, "FRIENDSHIP_OFFER_NOT_FOUND");

        assertThat(offerStatus(aliceId, borisId)).isEqualTo("PENDING");
        assertThat(friendshipsOf(offerId)).isZero();
        assertThat(subscriptions(borisId, aliceId)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"accept", "reject"})
    @DisplayName("accept/reject несуществующей заявки: 404 FRIENDSHIP_OFFER_NOT_FOUND")
    void whenDecideMissingOfferThenNotFound(String action) throws Exception {
        expectError(
                decide(action, decideBody(UUID.randomUUID(), UUID.randomUUID())),
                404, "FRIENDSHIP_OFFER_NOT_FOUND"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "accept, accept, FRIENDSHIP_OFFER_ALREADY_ACCEPTED, ACCEPTED, 1",
            "accept, reject, FRIENDSHIP_OFFER_ALREADY_ACCEPTED, ACCEPTED, 1",
            "reject, accept, FRIENDSHIP_OFFER_ALREADY_REJECTED, REJECTED, 0",
            "reject, reject, FRIENDSHIP_OFFER_ALREADY_REJECTED, REJECTED, 0"
    })
    @DisplayName("Повторное или перекрёстное решение: 409 с кодом текущего статуса, данные и дубликаты не меняются")
    void whenDecideDecidedOfferThenConflict(
            String first, String second, String code, String offerStatus, int friendships
    ) throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);
        decide(first, decideBody(offerId, borisId)).andExpect(status().isOk());

        expectError(decide(second, decideBody(offerId, borisId)), 409, code);

        assertThat(offerStatus(aliceId, borisId)).isEqualTo(offerStatus);
        assertThat(friendshipsOf(offerId)).isEqualTo(friendships);
        assertThat(subscriptions(aliceId, borisId)).isOne();
        assertThat(subscriptions(borisId, aliceId)).isEqualTo(friendships);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "accept | {\"actorId\":\"%2$s\"}",
            "accept | {\"offerId\":\"%1$s\"}",
            "reject | {\"offerId\":\"42\",\"actorId\":\"%2$s\"}",
            "reject | {not json"
    })
    @DisplayName("accept/reject с некорректным телом: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
    void whenDecideWithInvalidBodyThenBadRequest(String action, String template) throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);

        expectError(
                decide(action, template.formatted(offerId, borisId)),
                400, "INVALID_FRIENDSHIP_OFFER_REQUEST"
        );
        assertThat(offerStatus(aliceId, borisId)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("После accept встречная заявка Борис -> Алиса: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
    void whenCounterOfferAfterAcceptThenConflict() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);
        decide("accept", decideBody(offerId, borisId)).andExpect(status().isOk());

        expectError(create(body(borisId, aliceId)), 409, "FRIENDSHIP_OFFER_ALREADY_ACCEPTED");

        assertThat(offersFrom(borisId)).isZero();
        assertThat(friendshipsOf(offerId)).isOne();
    }

    private UUID createOffer(UUID fromUserId, UUID toUserId) throws Exception {
        var json = create(body(fromUserId, toUserId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions decide(String action, String json) throws Exception {
        return mockMvc.perform(post(BASE + "/" + action)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String decideBody(UUID offerId, UUID actorId) {
        return "{\"offerId\":\"%s\",\"actorId\":\"%s\"}".formatted(offerId, actorId);
    }

    private int friendshipsOf(UUID offerId) {
        return count("SELECT count(*) FROM friendships WHERE friendship_offer_id = ?", offerId);
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
        return count("SELECT count(*) FROM friendship_offer WHERE from_user_id = ?", userId);
    }

    private int offersTo(UUID userId) {
        return count("SELECT count(*) FROM friendship_offer WHERE to_user_id = ?", userId);
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
                "SELECT status FROM friendship_offer WHERE from_user_id = ? AND to_user_id = ?",
                String.class, fromUserId, toUserId
        );
    }

    private int count(String sql, Object... args) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, args));
    }
}
