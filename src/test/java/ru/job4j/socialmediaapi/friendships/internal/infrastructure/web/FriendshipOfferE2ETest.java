package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.job4j.socialmediaapi.E2ETest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.RejectFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferCursorWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferPageWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests of the friendship offer API ({@code docs/api/modules/friendships.yaml}): real HTTP requests
 * to the running application with PostgreSQL in Testcontainers. One nested group per operation:
 * successful requests, validation errors and forbidden state transitions
 * ({@code PENDING → ACCEPTED | REJECTED}, a decided offer cannot be decided again).
 * Every test uses new random users, so the tests do not depend on each other or on their order.
 */
@E2ETest
@DisplayName("E2E: API заявок в друзья /api/v1/friendship-offer")
class FriendshipOfferE2ETest {

    /** Base URL of the friendship offer endpoints. */
    private static final String BASE = "/api/v1/friendship-offer";

    /** Expected body of {@code 400} for an invalid body or parameters (contract example). */
    private static final ErrorResponse INVALID_REQUEST =
            new ErrorResponse("INVALID_FRIENDSHIP_OFFER_REQUEST", "Некорректное тело или параметры запроса");

    /** Expected body of {@code 400} for a missing or invalid {@code userId} (contract example). */
    private static final ErrorResponse INVALID_USER_ID =
            new ErrorResponse("INVALID_USER_ID", "Некорректный или отсутствующий параметр userId");

    /** Expected body of {@code 200} for a repeated or counter offer while the pair's offer is pending. */
    private static final ErrorResponse ALREADY_PENDING =
            new ErrorResponse("FRIENDSHIP_OFFER_ALREADY_PENDING", "Предложение дружбы уже отправлено");

    /** Expected body of {@code 404}: no offer, or the actor is not its recipient. */
    private static final ErrorResponse NOT_FOUND =
            new ErrorResponse("FRIENDSHIP_OFFER_NOT_FOUND", "Предложение дружбы не найдено");

    /** Expected body of {@code 409} for an accepted offer. */
    private static final ErrorResponse ALREADY_ACCEPTED =
            new ErrorResponse("FRIENDSHIP_OFFER_ALREADY_ACCEPTED", "Предложение дружбы уже принято");

    /** Expected body of {@code 409} for a rejected offer. */
    private static final ErrorResponse ALREADY_REJECTED =
            new ErrorResponse("FRIENDSHIP_OFFER_ALREADY_REJECTED", "Предложение дружбы уже отклонено");

    /** HTTP client bound to the random port of the running application. */
    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("POST /create")
    class Create {

        @Test
        @DisplayName("Новая пара: 201 и заявка PENDING")
        void whenNewPairThenCreatedPending() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();

            var response = restTemplate.postForEntity(BASE + "/create",
                    new CreateFriendshipOfferWebRequest(fromUserId, toUserId), CreateFriendshipOfferWebResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            var expected = new CreateFriendshipOfferWebResponse()
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .status(CreateFriendshipOfferWebResponse.StatusEnum.PENDING);
            assertThat(response.getBody())
                    .usingRecursiveComparison()
                    .ignoringFields("id", "createdAt")
                    .isEqualTo(expected);
            assertThat(response.getBody().getId()).isNotNull();
            assertThat(response.getBody().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Повтор той же заявки, пока она PENDING: 200 FRIENDSHIP_OFFER_ALREADY_PENDING")
        void whenRepeatedWhilePendingThenAlreadyPending() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();
            createOffer(fromUserId, toUserId);

            expectError(create(fromUserId, toUserId), HttpStatus.OK, ALREADY_PENDING);
        }

        @Test
        @DisplayName("Встречная заявка, пока первая PENDING: 200 FRIENDSHIP_OFFER_ALREADY_PENDING")
        void whenCounterOfferWhilePendingThenAlreadyPending() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();
            createOffer(fromUserId, toUserId);

            expectError(create(toUserId, fromUserId), HttpStatus.OK, ALREADY_PENDING);
        }

        @Test
        @DisplayName("Повтор и встречная заявка после accept: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
        void whenOfferAcceptedThenRepeatAndCounterConflict() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, toUserId);
            accept(offer.getId(), toUserId);

            expectError(create(fromUserId, toUserId), HttpStatus.CONFLICT, ALREADY_ACCEPTED);
            expectError(create(toUserId, fromUserId), HttpStatus.CONFLICT, ALREADY_ACCEPTED);
        }

        @Test
        @DisplayName("Повтор и встречная заявка после reject: 409 FRIENDSHIP_OFFER_ALREADY_REJECTED")
        void whenOfferRejectedThenRepeatAndCounterConflict() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, toUserId);
            reject(offer.getId(), toUserId);

            expectError(create(fromUserId, toUserId), HttpStatus.CONFLICT, ALREADY_REJECTED);
            expectError(create(toUserId, fromUserId), HttpStatus.CONFLICT, ALREADY_REJECTED);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidBodies")
        @DisplayName("Некорректное тело: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
        void whenInvalidBodyThenBadRequest(String situation, String body) {
            expectError(postJson("/create", body), HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        /**
         * @return situation and request body of the invalid create requests of the contract
         */
        static Stream<Arguments> invalidBodies() {
            var from = UUID.randomUUID();
            var to = UUID.randomUUID();
            return Stream.of(
                    Arguments.of("тело не читается", "{"),
                    Arguments.of("нет fromUserId", "{\"toUserId\":\"%s\"}".formatted(to)),
                    Arguments.of("нет toUserId", "{\"fromUserId\":\"%s\"}".formatted(from)),
                    Arguments.of("fromUserId null", "{\"fromUserId\":null,\"toUserId\":\"%s\"}".formatted(to)),
                    Arguments.of("fromUserId не UUID", "{\"fromUserId\":\"42\",\"toUserId\":\"%s\"}".formatted(to)),
                    Arguments.of("toUserId не UUID", "{\"fromUserId\":\"%s\",\"toUserId\":\"x\"}".formatted(from)),
                    Arguments.of("заявка самому себе",
                            "{\"fromUserId\":\"%1$s\",\"toUserId\":\"%1$s\"}".formatted(from))
            );
        }
    }

    @Nested
    @DisplayName("POST /accept")
    class Accept {

        @Test
        @DisplayName("Получатель принимает PENDING-заявку: 200 и заявка ACCEPTED с временем принятия")
        void whenRecipientAcceptsThenAccepted() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, toUserId);

            var response = accept(offer.getId(), toUserId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var expected = new AcceptFriendshipOfferWebResponse()
                    .id(offer.getId())
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .status(AcceptFriendshipOfferWebResponse.StatusEnum.ACCEPTED)
                    .createdAt(offer.getCreatedAt());
            assertThat(response.getBody())
                    .usingRecursiveComparison()
                    .ignoringFields("acceptedAt")
                    .isEqualTo(expected);
            assertThat(response.getBody().getAcceptedAt()).isAfter(offer.getCreatedAt());
        }

        @Test
        @DisplayName("Повторное принятие: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
        void whenAcceptedTwiceThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            accept(offer.getId(), toUserId);

            expectError(accept(offer.getId(), toUserId, ErrorResponse.class), HttpStatus.CONFLICT, ALREADY_ACCEPTED);
        }

        @Test
        @DisplayName("Принятие отклонённой заявки: 409 FRIENDSHIP_OFFER_ALREADY_REJECTED")
        void whenAcceptRejectedThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            reject(offer.getId(), toUserId);

            expectError(accept(offer.getId(), toUserId, ErrorResponse.class), HttpStatus.CONFLICT, ALREADY_REJECTED);
        }

        @Test
        @DisplayName("Принимает не получатель (отправитель или посторонний): 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenActorIsNotRecipientThenNotFound() {
            var fromUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, UUID.randomUUID());

            expectError(accept(offer.getId(), fromUserId, ErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
            expectError(accept(offer.getId(), UUID.randomUUID(), ErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @Test
        @DisplayName("Заявки нет: 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenOfferMissingThenNotFound() {
            expectError(accept(UUID.randomUUID(), UUID.randomUUID(), ErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ru.job4j.socialmediaapi.friendships.internal.infrastructure.web"
                + ".FriendshipOfferE2ETest#invalidDecisionBodies")
        @DisplayName("Некорректное тело: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
        void whenInvalidBodyThenBadRequest(String situation, String body) {
            expectError(postJson("/accept", body), HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /reject")
    class Reject {

        @Test
        @DisplayName("Получатель отклоняет PENDING-заявку: 200 и заявка REJECTED с временем отклонения")
        void whenRecipientRejectsThenRejected() {
            var fromUserId = UUID.randomUUID();
            var toUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, toUserId);

            var response = reject(offer.getId(), toUserId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var expected = new RejectFriendshipOfferWebResponse()
                    .id(offer.getId())
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .status(RejectFriendshipOfferWebResponse.StatusEnum.REJECTED)
                    .createdAt(offer.getCreatedAt());
            assertThat(response.getBody())
                    .usingRecursiveComparison()
                    .ignoringFields("rejectedAt")
                    .isEqualTo(expected);
            assertThat(response.getBody().getRejectedAt()).isAfter(offer.getCreatedAt());
        }

        @Test
        @DisplayName("Повторное отклонение: 409 FRIENDSHIP_OFFER_ALREADY_REJECTED")
        void whenRejectedTwiceThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            reject(offer.getId(), toUserId);

            expectError(reject(offer.getId(), toUserId, ErrorResponse.class), HttpStatus.CONFLICT, ALREADY_REJECTED);
        }

        @Test
        @DisplayName("Отклонение принятой заявки: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
        void whenRejectAcceptedThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            accept(offer.getId(), toUserId);

            expectError(reject(offer.getId(), toUserId, ErrorResponse.class), HttpStatus.CONFLICT, ALREADY_ACCEPTED);
        }

        @Test
        @DisplayName("Отклоняет не получатель (отправитель или посторонний): 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenActorIsNotRecipientThenNotFound() {
            var fromUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, UUID.randomUUID());

            expectError(reject(offer.getId(), fromUserId, ErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
            expectError(reject(offer.getId(), UUID.randomUUID(), ErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @Test
        @DisplayName("Заявки нет: 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenOfferMissingThenNotFound() {
            expectError(reject(UUID.randomUUID(), UUID.randomUUID(), ErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ru.job4j.socialmediaapi.friendships.internal.infrastructure.web"
                + ".FriendshipOfferE2ETest#invalidDecisionBodies")
        @DisplayName("Некорректное тело: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
        void whenInvalidBodyThenBadRequest(String situation, String body) {
            expectError(postJson("/reject", body), HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /{offerId}")
    class GetById {

        @Test
        @DisplayName("PENDING-заявка: 200 и все поля, updatedAt = createdAt")
        void whenPendingOfferThenOk() {
            var offer = createOffer(UUID.randomUUID(), UUID.randomUUID());

            var response = getOffer(offer.getId().toString(), FriendshipOfferWebResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(pending(offer));
        }

        @Test
        @DisplayName("Принятая заявка: 200, статус ACCEPTED, updatedAt = acceptedAt")
        void whenAcceptedOfferThenOk() {
            var offer = createOffer(UUID.randomUUID(), UUID.randomUUID());
            var accepted = accept(offer.getId(), offer.getToUserId()).getBody();

            var response = getOffer(offer.getId().toString(), FriendshipOfferWebResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(pending(offer)
                    .status(FriendshipOfferWebResponse.StatusEnum.ACCEPTED)
                    .updatedAt(accepted.getAcceptedAt()));
        }

        @Test
        @DisplayName("Заявки нет: 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenOfferMissingThenNotFound() {
            expectError(getOffer(UUID.randomUUID().toString(), ErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @Test
        @DisplayName("id не UUID: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
        void whenIdNotUuidThenBadRequest() {
            expectError(getOffer("42", ErrorResponse.class), HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        private <T> ResponseEntity<T> getOffer(String offerId, Class<T> type) {
            return restTemplate.getForEntity(BASE + "/{offerId}", type, offerId);
        }
    }

    @Nested
    @DisplayName("GET (страница всех заявок)")
    class GetPage {

        @Test
        @DisplayName("Страница: новые заявки первыми, next — последняя заявка страницы, по next — следующая")
        void whenPagedThenNewestFirstAndNextLeadsOn() {
            var first = pending(createOffer(UUID.randomUUID(), UUID.randomUUID()));
            var second = pending(createOffer(UUID.randomUUID(), UUID.randomUUID()));
            var third = pending(createOffer(UUID.randomUUID(), UUID.randomUUID()));

            var page = restTemplate.getForEntity(BASE + "?limit=2", FriendshipOfferPageWebResponse.class);

            assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(page.getBody()).isEqualTo(new FriendshipOfferPageWebResponse(
                    List.of(third, second),
                    new FriendshipOfferCursorWebResponse(second.getCreatedAt(), second.getId())));

            var next = restTemplate.getForEntity(BASE + "?limit=2&afterCreatedAt={createdAt}&afterId={id}",
                    FriendshipOfferPageWebResponse.class,
                    page.getBody().getNext().getCreatedAt(), page.getBody().getNext().getId());

            assertThat(next.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(next.getBody().getOffers()).first().isEqualTo(first);
        }

        @Test
        @DisplayName("Без limit: 20 заявок (значение по умолчанию) и курсор следующей страницы")
        void whenNoLimitThenDefaultLimit() {
            for (int i = 0; i < 21; i++) {
                createOffer(UUID.randomUUID(), UUID.randomUUID());
            }

            var page = restTemplate.getForEntity(BASE, FriendshipOfferPageWebResponse.class);

            assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(page.getBody().getOffers()).hasSize(20);
            assertThat(page.getBody().getNext()).isNotNull();
        }

        @Test
        @DisplayName("После самой старой заявки: 200, пустая страница и next = null")
        void whenNothingAfterCursorThenEmptyPage() {
            var page = restTemplate.getForEntity(BASE + "?afterCreatedAt={createdAt}&afterId={id}",
                    FriendshipOfferPageWebResponse.class, "1970-01-01T00:00:00Z", UUID.randomUUID());

            assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(page.getBody()).isEqualTo(new FriendshipOfferPageWebResponse(List.of(), null));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "limit=0",
                "limit=101",
                "limit=abc",
                "afterCreatedAt=yesterday&afterId=00000000-0000-0000-0000-000000000001",
                "afterCreatedAt=2026-10-12T10:15:00Z&afterId=42",
                "afterCreatedAt=2026-10-12T10:15:00Z",
                "afterId=00000000-0000-0000-0000-000000000001"
        })
        @DisplayName("Некорректные параметры: 400 INVALID_FRIENDSHIP_OFFER_REQUEST")
        void whenInvalidParamsThenBadRequest(String query) {
            expectError(restTemplate.getForEntity(BASE + "?" + query, ErrorResponse.class),
                    HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /incoming, /outgoing")
    class IncomingOutgoing {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/incoming", "/incoming?userId=1", "/outgoing", "/outgoing?userId=1"})
        @DisplayName("userId нет или не UUID: 400 INVALID_USER_ID")
        void whenUserIdMissingOrNotUuidThenBadRequest(String path) {
            expectError(restTemplate.getForEntity(BASE + path, ErrorResponse.class),
                    HttpStatus.BAD_REQUEST, INVALID_USER_ID);
        }
    }

    /**
     * @return situation and request body of the invalid accept / reject requests of the contract
     */
    static Stream<Arguments> invalidDecisionBodies() {
        var offerId = UUID.randomUUID();
        var actorId = UUID.randomUUID();
        return Stream.of(
                Arguments.of("тело не читается", "{"),
                Arguments.of("нет offerId", "{\"actorId\":\"%s\"}".formatted(actorId)),
                Arguments.of("нет actorId", "{\"offerId\":\"%s\"}".formatted(offerId)),
                Arguments.of("offerId null", "{\"offerId\":null,\"actorId\":\"%s\"}".formatted(actorId)),
                Arguments.of("offerId не UUID", "{\"offerId\":\"42\",\"actorId\":\"%s\"}".formatted(actorId)),
                Arguments.of("actorId не UUID", "{\"offerId\":\"%s\",\"actorId\":\"x\"}".formatted(offerId))
        );
    }

    private CreateFriendshipOfferWebResponse createOffer(UUID fromUserId, UUID toUserId) {
        var response = restTemplate.postForEntity(BASE + "/create",
                new CreateFriendshipOfferWebRequest(fromUserId, toUserId), CreateFriendshipOfferWebResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<ErrorResponse> create(UUID fromUserId, UUID toUserId) {
        return restTemplate.postForEntity(BASE + "/create",
                new CreateFriendshipOfferWebRequest(fromUserId, toUserId), ErrorResponse.class);
    }

    private ResponseEntity<AcceptFriendshipOfferWebResponse> accept(UUID offerId, UUID actorId) {
        return accept(offerId, actorId, AcceptFriendshipOfferWebResponse.class);
    }

    private <T> ResponseEntity<T> accept(UUID offerId, UUID actorId, Class<T> type) {
        return restTemplate.postForEntity(BASE + "/accept",
                new AcceptFriendshipOfferWebRequest(offerId, actorId), type);
    }

    private ResponseEntity<RejectFriendshipOfferWebResponse> reject(UUID offerId, UUID actorId) {
        return reject(offerId, actorId, RejectFriendshipOfferWebResponse.class);
    }

    private <T> ResponseEntity<T> reject(UUID offerId, UUID actorId, Class<T> type) {
        return restTemplate.postForEntity(BASE + "/reject",
                new RejectFriendshipOfferWebRequest(offerId, actorId), type);
    }

    private ResponseEntity<ErrorResponse> postJson(String path, String json) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(BASE + path, new HttpEntity<>(json, headers), ErrorResponse.class);
    }

    private static FriendshipOfferWebResponse pending(CreateFriendshipOfferWebResponse offer) {
        return new FriendshipOfferWebResponse(offer.getId(), offer.getFromUserId(), offer.getToUserId(),
                FriendshipOfferWebResponse.StatusEnum.PENDING, offer.getCreatedAt(), offer.getCreatedAt());
    }

    private static void expectError(ResponseEntity<?> response, HttpStatus status, ErrorResponse expected) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
