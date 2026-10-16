package ru.job4j.socialmediaapi.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;

/**
 * Errors common to all modules. Consulted after the modules' own handlers
 * ({@code *ExceptionHandler} in {@code <module>.internal.infrastructure.web}).
 */
@RestControllerAdvice
@Order()
public class GlobalExceptionHandler {

    /** {@code code} of the response body for an unexpected server error. */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    /** {@code message} of the response body for an unexpected server error. */
    public static final String INTERNAL_ERROR_MESSAGE = "Внутренняя ошибка сервера";

    /** Logs unexpected errors that are hidden from the client. */
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * A database constraint fired although the application checks the rules before:
     * this is a bug, not a business outcome. The PostgreSQL message (with the
     * constraint name) goes to the log, the client gets a generic body.
     *
     * @param exception violation reported by Spring JDBC
     * @return {@code 500} with the INTERNAL_ERROR error body
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        LOG.error("Unexpected database constraint violation: {}",
                exception.getMostSpecificCause().getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(INTERNAL_ERROR, INTERNAL_ERROR_MESSAGE));
    }
}
