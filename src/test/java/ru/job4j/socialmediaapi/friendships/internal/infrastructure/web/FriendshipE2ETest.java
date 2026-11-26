package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ru.job4j.socialmediaapi.E2ETest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendPageWebResponse;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-тесты {@code GET /api/v1/friendships}: настоящие HTTP-запросы к запущенному приложению с PostgreSQL
 * в Testcontainers. Дружба создаётся через API (create + accept). Каждый тест использует новых случайных пользователей.
 */
@E2ETest
@DisplayName("E2E: список друзей GET /api/v1/friendships")
class FriendshipE2ETest extends PostgreSqlIntegrationTest {

    /** URL страницы друзей. */
    private static final String FRIENDS = "/api/v1/friendships";

    /** Базовый URL эндпоинтов заявок в друзья. */
    private static final String OFFERS = "/api/v1/friendship-offer";

    /** HTTP-клиент, привязанный к случайному порту запущенного приложения. */
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("После accept: друг виден обоим участникам, id дружбы общий, userId — другой участник")
    void whenOfferAcceptedThenFriendOfBothUsers() {
        var sender = UUID.randomUUID();
        var recipient = UUID.randomUUID();
        befriend(sender, recipient);

        var ofSender = getPage(sender);
        var ofRecipient = getPage(recipient);

        assertThat(ofSender.getNext()).isNull();
        assertThat(ofSender.getFriends()).singleElement()
                .satisfies(friend -> assertThat(friend.getUserId()).isEqualTo(recipient));
        assertThat(ofRecipient.getFriends()).singleElement()
                .satisfies(friend -> {
                    assertThat(friend.getUserId()).isEqualTo(sender);
                    assertThat(friend.getId()).isEqualTo(ofSender.getFriends().getFirst().getId());
                });
    }

    @Test
    @DisplayName("Страницы: новые дружбы первыми, по next — следующая страница")
    void whenPagedThenNewestFirstAndNextLeadsOn() {
        var user = UUID.randomUUID();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        befriend(user, first);
        befriend(second, user);

        var page = restTemplate.getForEntity(FRIENDS + "?userId={userId}&limit=1",
                FriendPageWebResponse.class, user).getBody();
        assert page != null;
        assertThat(page.getFriends()).singleElement()
                .satisfies(friend -> assertThat(friend.getUserId()).isEqualTo(second));
        assertThat(page.getNext()).isNotNull();

        var last = restTemplate.getForEntity(FRIENDS + "?userId={userId}&limit=1&afterCreatedAt={at}&afterId={id}",
                FriendPageWebResponse.class, user, page.getNext().getCreatedAt(), page.getNext().getId()).getBody();
        assert last != null;
        assertThat(last.getFriends()).singleElement()
                .satisfies(friend -> assertThat(friend.getUserId()).isEqualTo(first));
        assertThat(last.getNext()).isNull();
    }

    @Test
    @DisplayName("Нет друзей (или неизвестный пользователь): 200, пустая страница и next = null")
    void whenNoFriendsThenEmptyPage() {
        assertThat(getPage(UUID.randomUUID())).isEqualTo(new FriendPageWebResponse(List.of(), null));
    }

    @Test
    @DisplayName("userId не UUID: 400 VALIDATION_ERROR")
    void whenUserIdNotUuidThenBadRequest() {
        var response = restTemplate.getForEntity(FRIENDS + "?userId=abc", ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(
                new ApiErrorResponse(400, "Некорректное тело или параметры запроса", "VALIDATION_ERROR"));
    }

    private FriendPageWebResponse getPage(UUID userId) {
        var response = restTemplate.getForEntity(FRIENDS + "?userId={userId}", FriendPageWebResponse.class, userId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assert response.getBody() != null;
        return response.getBody();
    }

    private void befriend(UUID sender, UUID recipient) {
        var created = restTemplate.postForEntity(OFFERS + "/create",
                asUser(sender, new CreateFriendshipOfferWebRequest(recipient)), CreateFriendshipOfferWebResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assert created.getBody() != null;
        var accepted = restTemplate.postForEntity(OFFERS + "/accept",
                asUser(recipient, new AcceptFriendshipOfferWebRequest(created.getBody().getId())),
                AcceptFriendshipOfferWebResponse.class);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static <T> HttpEntity<T> asUser(UUID userId, T body) {
        var headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        return new HttpEntity<>(body, headers);
    }
}
