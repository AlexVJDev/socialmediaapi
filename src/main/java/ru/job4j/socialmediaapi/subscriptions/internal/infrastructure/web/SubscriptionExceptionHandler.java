package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;
import ru.job4j.socialmediaapi.shared.web.CommonErrorCode;

/**
 * Errors of the subscription endpoints → HTTP responses of the API contract.
 * Applies only to {@link SubscriptionController}; it is consulted before the common
 * {@code shared.exception.GlobalExceptionHandler}, which answers unexpected errors with {@code 500}.
 */
@RestControllerAdvice(assignableTypes = SubscriptionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SubscriptionExceptionHandler {

    /**
     * The only parameter of the endpoints, {@code userId}, is missing or empty
     * ({@link MissingServletRequestParameterException}) or not a UUID ({@link MethodArgumentTypeMismatchException}).
     *
     * @return {@code 400} with the common VALIDATION_ERROR error body
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidUserId() {
        return CommonErrorCode.VALIDATION_ERROR.response();
    }
}
