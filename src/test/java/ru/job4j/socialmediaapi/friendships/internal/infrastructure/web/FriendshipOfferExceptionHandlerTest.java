package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Обработчик ошибок заявок в друзья: повтор заявки, дубликат и заявка не найдена")
class FriendshipOfferExceptionHandlerTest {

    /** Handler under test. */
    private final FriendshipOfferExceptionHandler handler = new FriendshipOfferExceptionHandler();

    @Test
    @DisplayName("FriendshipOfferAlreadyPendingException даёт 200 с телом FRIENDSHIP_OFFER_ALREADY_PENDING")
    void whenOfferAlreadyPendingThenOkWithBody() {
        var response = handler.handleAlreadyPending();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
                200, "Предложение дружбы уже отправлено", "FRIENDSHIP_OFFER_ALREADY_PENDING"
        ));
    }

    @Test
    @DisplayName("FriendshipOfferAlreadyExistsException (статус заявки не прочитан) "
            + "даёт 409 FRIENDSHIP_OFFER_ALREADY_EXISTS")
    void whenOfferAlreadyExistsThenConflict() {
        var exception = new FriendshipOfferAlreadyExistsException(UUID.randomUUID(), UUID.randomUUID(), null);

        var response = handler.handleAlreadyExists(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
                409, "Предложение дружбы для этой пары уже существует", "FRIENDSHIP_OFFER_ALREADY_EXISTS"
        ));
    }

    @Test
    @DisplayName("FriendshipRequestNotFoundException даёт 404 FRIENDSHIP_OFFER_NOT_FOUND")
    void whenOfferNotFoundThenNotFound() {
        var response = handler.handleNotFound();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
                404, "Предложение дружбы не найдено", "FRIENDSHIP_OFFER_NOT_FOUND"
        ));
    }
}
