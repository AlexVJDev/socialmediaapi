package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipOfferJpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("POST /api/v1/friendship-offer/{create,accept,reject}: ответы и ошибки по friendship_transitions.puml")
class FriendshipOfferControllerTest {

    /** Базовый URL операций с заявками в друзья. */
    private static final String BASE = "/api/v1/friendship-offer";

    /** Заголовок с текущим пользователем: отправитель, принимающий или отклоняющий заявку. */
    private static final String USER_HEADER = "X-User-Id";

    /** URL создания заявки. */
    private static final String CREATE = BASE + "/create";

    /** MockMvc: вызовы API без запущенного сервера. */
    @Autowired
    private MockMvc mockMvc;

    /** Прямой доступ к БД для подготовки и проверки данных. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Чтение заявок через Spring Data JPA: результат HTTP-запроса проверяется по сохранённой строке. */
    @Autowired
    private FriendshipOfferJpaRepository offers;

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "нет toUserId                | {}",
            "toUserId null               | {\"toUserId\":null}",
            "toUserId не UUID            | {\"toUserId\":\"not-a-uuid\"}",
            "toUserId пустая строка      | {\"toUserId\":\"\"}",
            "toUserId UUID без дефисов   | {\"toUserId\":\"0b6f3c9e6a1d4c2e9f5a2d7b8e1c4a01\"}",
            "toUserId число, не строка   | {\"toUserId\":123}"
    })
    @DisplayName("create с отсутствующим, null или не UUID toUserId: 400 VALIDATION_ERROR, "
            + "заявка и подписка не создаются")
    void whenCreateWithInvalidIdsThenBadRequest(String situation, String template) throws Exception {
        var fromUserId = UUID.randomUUID();

        expectError(create(fromUserId, template), 400, "VALIDATION_ERROR");

        assertThat(offersFrom(fromUserId)).isZero();
        assertThat(subscriptionsOf(fromUserId)).isZero();
    }

    @Test
    @DisplayName("Некорректный JSON: 400 VALIDATION_ERROR")
    void whenBodyNotJsonThenBadRequest() throws Exception {
        expectError(create(UUID.randomUUID(), "{not json"), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Заявка самому себе (toUserId = X-User-Id): 400 VALIDATION_ERROR, заявка и подписка не создаются")
    void whenOfferToSelfThenBadRequest() throws Exception {
        var userId = UUID.randomUUID();

        expectError(create(userId, body(userId)), 400, "VALIDATION_ERROR");
        assertThat(offersFrom(userId)).isZero();
        assertThat(subscriptionsOf(userId)).isZero();
    }

    @Test
    @DisplayName("Новая заявка: 201, PENDING, создаются заявка и подписка инициатора на получателя")
    void whenNewOfferThenCreated() throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();

        var json = create(fromUserId, body(toUserId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromUserId").value(fromUserId.toString()))
                .andExpect(jsonPath("$.toUserId").value(toUserId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        var stored = offers.findById(UUID.fromString(JsonPath.read(json, "$.id"))).orElseThrow();
        assertThat(stored.getFromUserId()).isEqualTo(fromUserId);
        assertThat(stored.getToUserId()).isEqualTo(toUserId);
        assertThat(stored.getStatus()).isEqualTo(FriendshipOfferStatus.PENDING);
        assertThat(OffsetDateTime.parse(JsonPath.read(json, "$.createdAt")).toInstant())
                .isEqualTo(stored.getCreatedAt());
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    @Test
    @DisplayName("Повтор заявки в статусе PENDING: 200 FRIENDSHIP_OFFER_ALREADY_PENDING, данные не меняются")
    void whenRepeatPendingOfferThenOkWithPendingCode() throws Exception {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var offerId = createOffer(fromUserId, toUserId);
        var created = offers.findById(offerId).orElseThrow();

        expectError(create(fromUserId, body(toUserId)), 200, "FRIENDSHIP_OFFER_ALREADY_PENDING");

        assertThat(offersFrom(fromUserId)).isOne();
        assertThat(offers.findById(offerId).orElseThrow()).usingRecursiveComparison().isEqualTo(created);
        assertThat(subscriptions(fromUserId, toUserId)).isOne();
    }

    @Test
    @DisplayName("Встречная заявка при PENDING: 200 FRIENDSHIP_OFFER_ALREADY_PENDING, "
            + "заявка пары одна, подписки получателя на инициатора нет")
    void whenCounterOfferPendingThenOkWithPendingCode() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        create(aliceId, body(borisId)).andExpect(status().isCreated());

        expectError(create(borisId, body(aliceId)), 200, "FRIENDSHIP_OFFER_ALREADY_PENDING");

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
        create(aliceId, body(borisId)).andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE friendship_offer SET status = ? WHERE from_user_id = ? AND to_user_id = ?",
                offerStatus, aliceId, borisId
        );

        expectError(create(borisId, body(aliceId)), 409, code);

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
        create(fromUserId, body(toUserId)).andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE friendship_offer SET status = ? WHERE from_user_id = ? AND to_user_id = ?",
                offerStatus, fromUserId, toUserId
        );

        expectError(create(fromUserId, body(toUserId)), 409, code);

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

        decide("accept", borisId, decideBody(offerId))
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

        decide("reject", borisId, decideBody(offerId))
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

        expectError(decide(action, actorId, decideBody(offerId)), 404, "FRIENDSHIP_OFFER_NOT_FOUND");

        assertThat(offerStatus(aliceId, borisId)).isEqualTo("PENDING");
        assertThat(friendshipsOf(offerId)).isZero();
        assertThat(subscriptions(borisId, aliceId)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"accept", "reject"})
    @DisplayName("accept/reject несуществующей заявки: 404 FRIENDSHIP_OFFER_NOT_FOUND")
    void whenDecideMissingOfferThenNotFound(String action) throws Exception {
        expectError(
                decide(action, UUID.randomUUID(), decideBody(UUID.randomUUID())),
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
        decide(first, borisId, decideBody(offerId)).andExpect(status().isOk());

        expectError(decide(second, borisId, decideBody(offerId)), 409, code);

        assertThat(offerStatus(aliceId, borisId)).isEqualTo(offerStatus);
        assertThat(friendshipsOf(offerId)).isEqualTo(friendships);
        assertThat(subscriptions(aliceId, borisId)).isOne();
        assertThat(subscriptions(borisId, aliceId)).isEqualTo(friendships);
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidDecideBodies")
    @DisplayName("accept/reject с отсутствующим, null или не UUID offerId: 400 VALIDATION_ERROR, "
            + "заявка остаётся PENDING, дружба и обратная подписка не создаются")
    void whenDecideWithInvalidBodyThenBadRequest(String action, String situation, String template) throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);

        expectError(
                decide(action, borisId, template.formatted(offerId)),
                400, "VALIDATION_ERROR"
        );
        assertThat(offerStatus(aliceId, borisId)).isEqualTo("PENDING");
        assertThat(friendshipsOf(offerId)).isZero();
        assertThat(subscriptions(borisId, aliceId)).isZero();
    }

    /**
     * Невалидные тела для accept и reject; {@code %1$s} — id заявки. Действующий — получатель из X-User-Id.
     *
     * @return действие, ситуация и шаблон тела
     */
    private static Stream<Arguments> invalidDecideBodies() {
        List<String[]> bodies = List.of(
                new String[] {"нет offerId", "{}"},
                new String[] {"offerId null", "{\"offerId\":null}"},
                new String[] {"offerId не UUID", "{\"offerId\":\"42\"}"},
                new String[] {"offerId пустая строка", "{\"offerId\":\"\"}"},
                new String[] {"offerId UUID без дефисов", "{\"offerId\":\"9c4e1b773f2a4d6c8b0e1a5f7d2c3e44\"}"},
                new String[] {"offerId число, не строка", "{\"offerId\":123}"},
                new String[] {"тело не JSON", "{not json"}
        );
        return Stream.of("accept", "reject")
                .flatMap(action -> bodies.stream().map(body -> Arguments.of(action, body[0], body[1])));
    }

    @Test
    @DisplayName("После accept встречная заявка Борис -> Алиса: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
    void whenCounterOfferAfterAcceptThenConflict() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var offerId = createOffer(aliceId, borisId);
        decide("accept", borisId, decideBody(offerId)).andExpect(status().isOk());

        expectError(create(borisId, body(aliceId)), 409, "FRIENDSHIP_OFFER_ALREADY_ACCEPTED");

        assertThat(offersFrom(borisId)).isZero();
        assertThat(friendshipsOf(offerId)).isOne();
    }

    private UUID createOffer(UUID fromUserId, UUID toUserId) throws Exception {
        var json = create(fromUserId, body(toUserId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions decide(String action, UUID actorId, String json) throws Exception {
        return mockMvc.perform(post(BASE + "/" + action)
                .header(USER_HEADER, actorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String decideBody(UUID offerId) {
        return "{\"offerId\":\"%s\"}".formatted(offerId);
    }

    private int friendshipsOf(UUID offerId) {
        return count("SELECT count(*) FROM friendships WHERE friendship_offer_id = ?", offerId);
    }

    private ResultActions create(UUID fromUserId, String json) throws Exception {
        return mockMvc.perform(post(CREATE)
                .header(USER_HEADER, fromUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static void expectError(ResultActions actions, int status, String code) throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private static String body(UUID toUserId) {
        return "{\"toUserId\":\"%s\"}".formatted(toUserId);
    }

    private int offersFrom(UUID userId) {
        return count("SELECT count(*) FROM friendship_offer WHERE from_user_id = ?", userId);
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
