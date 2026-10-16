package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Обработчик ошибок заявок в друзья: повтор заявки и заявка не найдена")
class FriendshipOfferExceptionHandlerTest {

    /** Body of the answer to a repeated offer of the pair. */
    private static final ErrorResponse PENDING_BODY = new ErrorResponse(
            "FRIENDSHIP_OFFER_ALREADY_PENDING", "Предложение дружбы уже отправлено"
    );

    /** Handler under test. */
    private final FriendshipOfferExceptionHandler handler = new FriendshipOfferExceptionHandler();

    @Test
    @DisplayName("FriendshipOfferAlreadyPendingException даёт 200 с телом FRIENDSHIP_OFFER_ALREADY_PENDING")
    void whenOfferAlreadyPendingThenOkWithBody() {
        var response = handler.handleAlreadyPending();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(PENDING_BODY);
    }

    @Test
    @DisplayName("FriendshipOfferAlreadyExistsException (статус заявки не прочитан) даёт тот же ответ, "
            + "что и PENDING: 200 с телом FRIENDSHIP_OFFER_ALREADY_PENDING")
    void whenOfferAlreadyExistsThenOkWithPendingBody() {
        var exception = new FriendshipOfferAlreadyExistsException(UUID.randomUUID(), UUID.randomUUID(), null);

        var response = handler.handleAlreadyExists(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(PENDING_BODY);
    }

    @Test
    @DisplayName("FriendshipRequestNotFoundException даёт 404 FRIENDSHIP_OFFER_NOT_FOUND")
    void whenOfferNotFoundThenNotFound() {
        var response = handler.handleNotFound();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(
                "FRIENDSHIP_OFFER_NOT_FOUND", "Предложение дружбы не найдено"
        ));
    }
}
