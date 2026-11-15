package ru.job4j.socialmediaapi.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;
import ru.job4j.socialmediaapi.shared.web.CommonErrorCode;

/**
 * Errors common to all modules. Consulted after the modules' own handlers
 * ({@code *ExceptionHandler} in {@code <module>.internal.infrastructure.web}).
 * <p>
 * The standard Spring MVC errors (unknown URL, unsupported method, {@code Content-Type} or {@code Accept}, …)
 * are answered by {@link ResponseEntityExceptionHandler} with their own status and a Spring {@code ProblemDetail}
 * body; otherwise {@link #handleUnexpected(Exception)} would turn them into {@code 500}.
 */
@RestControllerAdvice
@Order()
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

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
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        LOG.error("Unexpected database constraint violation: {}",
                exception.getMostSpecificCause().getMessage(), exception);
        return CommonErrorCode.INTERNAL_ERROR.response();
    }

    /**
     * Any other exception not handled by a module or by Spring MVC: a bug or a failure of the infrastructure.
     * The exception goes to the log; its message is not returned, as it may hold SQL or class names.
     *
     * @param exception unexpected exception
     * @return {@code 500} with the INTERNAL_ERROR error body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        LOG.error("Unexpected error: {}", exception.getMessage(), exception);
        return CommonErrorCode.INTERNAL_ERROR.response();
    }
}
