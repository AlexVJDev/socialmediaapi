package ru.job4j.socialmediaapi.adapter.in.web.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.adapter.in.web.dto.ErrorResponse;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyExistsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Обработчик ошибок: повтор заявки и непредвиденные ошибки БД")
class GlobalExceptionHandlerTest {

    /** Handler under test. */
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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

    @Test
    @DisplayName("DataIntegrityViolationException даёт 500 с общим телом без деталей БД")
    void whenDataIntegrityViolationThenInternalErrorWithoutDetails() {
        var exception = new DataIntegrityViolationException(
                "violates check constraint \"chk_friendship_offer_different_users\""
        );

        var response = handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.message()
        ));
    }
}
