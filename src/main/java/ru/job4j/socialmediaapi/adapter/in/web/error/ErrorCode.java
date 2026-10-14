package ru.job4j.socialmediaapi.adapter.in.web.error;

import org.springframework.http.HttpStatus;

/**
 * Error codes of the API contract: the constant name is the {@code code} of the response body.
 */
public enum ErrorCode {

    /** Invalid request body, missing required fields or an offer to oneself. */
    INVALID_FRIENDSHIP_OFFER_REQUEST(
            HttpStatus.BAD_REQUEST,
            "Некорректное тело запроса, отсутствуют обязательные поля или пользователь предлагает дружбу самому себе"
    ),
    /** The friendship offer has already been accepted. */
    FRIENDSHIP_OFFER_ALREADY_ACCEPTED(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже принято"
    ),
    /** The friendship offer has already been rejected. */
    FRIENDSHIP_OFFER_ALREADY_REJECTED(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже отклонено"
    ),
    /** Unexpected server error; details are logged, not returned to the client. */
    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Внутренняя ошибка сервера"
    );

    /** HTTP status of the error response. */
    private final HttpStatus status;

    /** Human-readable {@code message} of the error response body. */
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
