package ru.job4j.socialmediaapi.adapter.in.web.error;

import org.springframework.http.HttpStatus;

/**
 * Error codes of the API contract: the constant name is the {@code code} of the response body.
 */
public enum ErrorCode {

    INVALID_FRIENDSHIP_OFFER_REQUEST(
            HttpStatus.BAD_REQUEST,
            "Некорректное тело запроса, отсутствуют обязательные поля или пользователь предлагает дружбу самому себе"
    ),
    FRIENDSHIP_OFFER_ALREADY_ACCEPTED(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже принято"
    ),
    FRIENDSHIP_OFFER_ALREADY_REJECTED(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже отклонено"
    );

    private final HttpStatus status;

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
