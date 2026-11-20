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
import org.springframework.http.HttpMethod;
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
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-тесты API заявок в друзья ({@code docs/api/modules/friendships.yaml}): настоящие HTTP-запросы
 * к запущенному приложению с PostgreSQL в Testcontainers. Одна вложенная группа на операцию:
 * успешные запросы, ошибки валидации и запрещённые переходы статусов
 * ({@code PENDING → ACCEPTED | REJECTED}, решённую заявку нельзя решить повторно).
 * Каждый тест использует новых случайных пользователей, поэтому тесты не зависят друг от друга и от порядка.
 */
@E2ETest
@DisplayName("E2E: API заявок в друзья /api/v1/friendship-offer")
class FriendshipOfferE2ETest {

    /** Базовый URL эндпоинтов заявок в друзья. */
    private static final String BASE = "/api/v1/friendship-offer";

    /** Ожидаемое тело {@code 400} для некорректного тела или параметров (пример из контракта). */
    private static final ApiErrorResponse INVALID_REQUEST =
            new ApiErrorResponse(400, "Некорректное тело или параметры запроса", "VALIDATION_ERROR");

    /** Заголовок текущего пользователя. */
    private static final String USER_HEADER = "X-User-Id";

    /** Ожидаемое тело {@code 401}: нет текущего пользователя (пример контракта {@code responses/unauthorized.yaml}). */
    private static final ApiErrorResponse UNAUTHORIZED =
            new ApiErrorResponse(401, "Пользователь не определён", "UNAUTHORIZED");

    /** Ожидаемое тело {@code 200} для повторной или встречной заявки, пока заявка пары ждёт решения. */
    private static final ApiErrorResponse ALREADY_PENDING =
            new ApiErrorResponse(200, "Предложение дружбы уже отправлено", "FRIENDSHIP_OFFER_ALREADY_PENDING");

    /** Ожидаемое тело {@code 404}: заявки нет или действующий пользователь не её получатель. */
    private static final ApiErrorResponse NOT_FOUND =
            new ApiErrorResponse(404, "Предложение дружбы не найдено", "FRIENDSHIP_OFFER_NOT_FOUND");

    /** Ожидаемое тело {@code 409} для принятой заявки. */
    private static final ApiErrorResponse ALREADY_ACCEPTED =
            new ApiErrorResponse(409, "Предложение дружбы уже принято", "FRIENDSHIP_OFFER_ALREADY_ACCEPTED");

    /** Ожидаемое тело {@code 409} для отклонённой заявки. */
    private static final ApiErrorResponse ALREADY_REJECTED =
            new ApiErrorResponse(409, "Предложение дружбы уже отклонено", "FRIENDSHIP_OFFER_ALREADY_REJECTED");

    /** HTTP-клиент, привязанный к случайному порту запущенного приложения. */
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
            assert response.getBody() != null;
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
        @DisplayName("Некорректное тело: 400 VALIDATION_ERROR")
        void whenInvalidBodyThenBadRequest(String situation, String body) {
            expectError(postJson("/create", body), HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        /**
         * @return ситуация и тело запроса для некорректных запросов create из контракта
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
            assert response.getBody() != null;
            assertThat(response.getBody().getAcceptedAt()).isAfter(offer.getCreatedAt());
        }

        @Test
        @DisplayName("Повторное принятие: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
        void whenAcceptedTwiceThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            accept(offer.getId(), toUserId);

            expectError(accept(offer.getId(), toUserId, ApiErrorResponse.class), HttpStatus.CONFLICT, ALREADY_ACCEPTED);
        }

        @Test
        @DisplayName("Принятие отклонённой заявки: 409 FRIENDSHIP_OFFER_ALREADY_REJECTED")
        void whenAcceptRejectedThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            reject(offer.getId(), toUserId);

            expectError(accept(offer.getId(), toUserId, ApiErrorResponse.class), HttpStatus.CONFLICT, ALREADY_REJECTED);
        }

        @Test
        @DisplayName("Принимает не получатель (отправитель или посторонний): 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenActorIsNotRecipientThenNotFound() {
            var fromUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, UUID.randomUUID());

            expectError(accept(offer.getId(), fromUserId, ApiErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
            expectError(accept(offer.getId(), UUID.randomUUID(), ApiErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @Test
        @DisplayName("Заявки нет: 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenOfferMissingThenNotFound() {
            expectError(accept(UUID.randomUUID(), UUID.randomUUID(), ApiErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ru.job4j.socialmediaapi.friendships.internal.infrastructure.web"
                + ".FriendshipOfferE2ETest#invalidDecisionBodies")
        @DisplayName("Некорректное тело: 400 VALIDATION_ERROR")
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
            assert response.getBody() != null;
            assertThat(response.getBody().getRejectedAt()).isAfter(offer.getCreatedAt());
        }

        @Test
        @DisplayName("Повторное отклонение: 409 FRIENDSHIP_OFFER_ALREADY_REJECTED")
        void whenRejectedTwiceThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            reject(offer.getId(), toUserId);

            expectError(reject(offer.getId(), toUserId, ApiErrorResponse.class), HttpStatus.CONFLICT, ALREADY_REJECTED);
        }

        @Test
        @DisplayName("Отклонение принятой заявки: 409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED")
        void whenRejectAcceptedThenConflict() {
            var toUserId = UUID.randomUUID();
            var offer = createOffer(UUID.randomUUID(), toUserId);
            accept(offer.getId(), toUserId);

            expectError(reject(offer.getId(), toUserId, ApiErrorResponse.class), HttpStatus.CONFLICT, ALREADY_ACCEPTED);
        }

        @Test
        @DisplayName("Отклоняет не получатель (отправитель или посторонний): 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenActorIsNotRecipientThenNotFound() {
            var fromUserId = UUID.randomUUID();
            var offer = createOffer(fromUserId, UUID.randomUUID());

            expectError(reject(offer.getId(), fromUserId, ApiErrorResponse.class), HttpStatus.NOT_FOUND, NOT_FOUND);
            expectError(reject(offer.getId(), UUID.randomUUID(), ApiErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @Test
        @DisplayName("Заявки нет: 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenOfferMissingThenNotFound() {
            expectError(reject(UUID.randomUUID(), UUID.randomUUID(), ApiErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ru.job4j.socialmediaapi.friendships.internal.infrastructure.web"
                + ".FriendshipOfferE2ETest#invalidDecisionBodies")
        @DisplayName("Некорректное тело: 400 VALIDATION_ERROR")
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
            assert accepted != null;
            assertThat(response.getBody()).isEqualTo(pending(offer)
                    .status(FriendshipOfferWebResponse.StatusEnum.ACCEPTED)
                    .updatedAt(accepted.getAcceptedAt()));
        }

        @Test
        @DisplayName("Заявки нет: 404 FRIENDSHIP_OFFER_NOT_FOUND")
        void whenOfferMissingThenNotFound() {
            expectError(getOffer(UUID.randomUUID().toString(), ApiErrorResponse.class),
                    HttpStatus.NOT_FOUND, NOT_FOUND);
        }

        @Test
        @DisplayName("id не UUID: 400 VALIDATION_ERROR")
        void whenIdNotUuidThenBadRequest() {
            expectError(getOffer("42", ApiErrorResponse.class), HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        private <T> ResponseEntity<T> getOffer(String offerId, Class<T> type) {
            return restTemplate.getForEntity(BASE + "/{offerId}", type, offerId);
        }
    }

    @Nested
    @DisplayName("GET (страница заявок текущего пользователя)")
    class GetPage {

        @Test
        @DisplayName("INCOMING: новые первыми, next — последняя заявка, по next — дальше; исходящие не попадают")
        void whenIncomingPagedThenNewestFirstAndNextLeadsOn() {
            var user = UUID.randomUUID();
            var first = pending(createOffer(UUID.randomUUID(), user));
            var second = pending(createOffer(UUID.randomUUID(), user));
            var third = pending(createOffer(UUID.randomUUID(), user));
            createOffer(user, UUID.randomUUID());

            var page = getPage(user, "?direction=INCOMING&limit=2");
            assertThat(page.getOffers()).containsExactly(third, second);
            assertThat(page.getNext())
                    .isEqualTo(new FriendshipOfferCursorWebResponse(second.getCreatedAt(), second.getId()));

            var last = getPage(user, "?direction=INCOMING&limit=2&afterCreatedAt={createdAt}&afterId={id}",
                    page.getNext().getCreatedAt(), page.getNext().getId());
            assertThat(last).isEqualTo(new FriendshipOfferPageWebResponse(List.of(first), null));
        }

        @Test
        @DisplayName("OUTGOING: исходящие пользователя; status=ACCEPTED — только принятые")
        void whenOutgoingThenOffersOfSenderInStatus() {
            var user = UUID.randomUUID();
            var recipient = UUID.randomUUID();
            var accepted = createOffer(user, recipient);
            assertThat(accept(accepted.getId(), recipient).getStatusCode()).isEqualTo(HttpStatus.OK);
            var waiting = pending(createOffer(user, UUID.randomUUID()));

            assertThat(getPage(user, "?direction=OUTGOING"))
                    .isEqualTo(new FriendshipOfferPageWebResponse(List.of(waiting), null));
            assertThat(getPage(user, "?direction=OUTGOING&status=ACCEPTED").getOffers())
                    .singleElement()
                    .satisfies(offer -> {
                        assertThat(offer.getId()).isEqualTo(accepted.getId());
                        assertThat(offer.getStatus()).isEqualTo(FriendshipOfferWebResponse.StatusEnum.ACCEPTED);
                    });
        }

        @Test
        @DisplayName("Нет заявок: 200, пустая страница и next = null")
        void whenNoOffersThenEmptyPage() {
            assertThat(getPage(UUID.randomUUID(), "?direction=INCOMING"))
                    .isEqualTo(new FriendshipOfferPageWebResponse(List.of(), null));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "",
                "?direction=FOO",
                "?direction=INCOMING&status=FOO",
                "?direction=INCOMING&limit=0",
                "?direction=INCOMING&limit=101",
                "?direction=INCOMING&afterCreatedAt=2026-10-12T10:15:00Z"
        })
        @DisplayName("Нет direction или некорректные параметры: 400 VALIDATION_ERROR")
        void whenInvalidParamsThenBadRequest(String query) {
            expectError(get(UUID.randomUUID().toString(), query, ApiErrorResponse.class),
                    HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        @Test
        @DisplayName("Без X-User-Id: 401 UNAUTHORIZED")
        void whenNoCurrentUserThenUnauthorized() {
            expectError(restTemplate.getForEntity(BASE + "?direction=INCOMING", ApiErrorResponse.class),
                    HttpStatus.UNAUTHORIZED, UNAUTHORIZED);
        }

        private FriendshipOfferPageWebResponse getPage(UUID user, String query, Object... uriVariables) {
            var response = get(user.toString(), query, FriendshipOfferPageWebResponse.class, uriVariables);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assert response.getBody() != null;
            return response.getBody();
        }

        private <T> ResponseEntity<T> get(String user, String query, Class<T> type, Object... uriVariables) {
            var headers = new HttpHeaders();
            headers.set(USER_HEADER, user);
            return restTemplate.exchange(BASE + query, HttpMethod.GET, new HttpEntity<>(headers), type, uriVariables);
        }
    }

    /**
     * @return ситуация и тело запроса для некорректных запросов accept / reject из контракта
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

    private ResponseEntity<ApiErrorResponse> create(UUID fromUserId, UUID toUserId) {
        return restTemplate.postForEntity(BASE + "/create",
                new CreateFriendshipOfferWebRequest(fromUserId, toUserId), ApiErrorResponse.class);
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

    private ResponseEntity<ApiErrorResponse> postJson(String path, String json) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(BASE + path, new HttpEntity<>(json, headers), ApiErrorResponse.class);
    }

    private static FriendshipOfferWebResponse pending(CreateFriendshipOfferWebResponse offer) {
        return new FriendshipOfferWebResponse(offer.getId(), offer.getFromUserId(), offer.getToUserId(),
                FriendshipOfferWebResponse.StatusEnum.PENDING, offer.getCreatedAt(), offer.getCreatedAt());
    }

    private static void expectError(ResponseEntity<?> response, HttpStatus status, ApiErrorResponse expected) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
