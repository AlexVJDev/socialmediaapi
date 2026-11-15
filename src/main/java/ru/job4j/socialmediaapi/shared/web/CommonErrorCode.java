package ru.job4j.socialmediaapi.shared.web;

import org.springframework.http.HttpStatus;

/**
 * Error codes common to all modules.
 */
public enum CommonErrorCode implements ErrorCode {

    /**
     * Invalid request: unreadable or invalid body, missing or invalid parameters,
     * a business rule on the input (an offer to oneself, a partial page cursor).
     */
    VALIDATION_ERROR(
            HttpStatus.BAD_REQUEST,
            "Некорректное тело или параметры запроса"
    ),
    /** Unexpected server error: the details are logged and are not disclosed to the client. */
    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Внутренняя ошибка сервера"
    );

    /** HTTP status of the error response. */
    private final HttpStatus status;

    /** Human-readable {@code detail} of the error response body. */
    private final String detail;

    CommonErrorCode(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String detail() {
        return detail;
    }
}
