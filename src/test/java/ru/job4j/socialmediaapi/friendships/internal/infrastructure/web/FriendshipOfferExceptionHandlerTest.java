package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.error.ErrorCode;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Обработчик ошибок заявок в друзья: повтор заявки")
class FriendshipOfferExceptionHandlerTest {

    /** Handler under test. */
    private final FriendshipOfferExceptionHandler handler = new FriendshipOfferExceptionHandler();

    @Test
    @DisplayName("FriendshipOfferAlreadyPendingException даёт 409 FRIENDSHIP_OFFER_ALREADY_PENDING")
    void whenOfferAlreadyPendingThenConflict() {
        var response = handler.handleAlreadyPending();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(
                ErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.name(), ErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.message()
        ));
    }

    @Test
    @DisplayName("FriendshipOfferAlreadyExistsException (статус заявки не прочитан) даёт 204 без тела")
    void whenOfferAlreadyExistsThenNoContent() {
        var exception = new FriendshipOfferAlreadyExistsException(UUID.randomUUID(), UUID.randomUUID(), null);

        var response = handler.handleAlreadyExists(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
