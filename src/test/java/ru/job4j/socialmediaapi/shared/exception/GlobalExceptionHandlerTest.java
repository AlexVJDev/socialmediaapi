package ru.job4j.socialmediaapi.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Общий обработчик ошибок: непредвиденные ошибки")
class GlobalExceptionHandlerTest {

    /** Body of every unexpected error: no details of the exception. */
    private static final ApiErrorResponse INTERNAL_ERROR_BODY =
            new ApiErrorResponse(500, "Внутренняя ошибка сервера", "INTERNAL_ERROR");

    /** Handler under test. */
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DataIntegrityViolationException даёт 500 INTERNAL_ERROR без деталей БД")
    void whenDataIntegrityViolationThenInternalErrorWithoutDetails() {
        var exception = new DataIntegrityViolationException(
                "violates check constraint \"chk_friendship_offer_different_users\""
        );

        var response = handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(INTERNAL_ERROR_BODY);
    }

    @Test
    @DisplayName("Любое другое исключение даёт 500 INTERNAL_ERROR без текста исключения")
    void whenUnexpectedExceptionThenInternalErrorWithoutMessage() {
        var response = handler.handleUnexpected(new IllegalStateException("select * from secret_table"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(INTERNAL_ERROR_BODY);
    }
}
