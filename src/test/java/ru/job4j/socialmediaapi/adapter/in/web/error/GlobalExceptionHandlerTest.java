package ru.job4j.socialmediaapi.adapter.in.web.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.adapter.in.web.dto.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Обработчик ошибок: непредвиденные ошибки БД")
class GlobalExceptionHandlerTest {

    /** Handler under test. */
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
