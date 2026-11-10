package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.job4j.socialmediaapi.posts.internal.application.exception.IncompleteFeedCursorException;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.error.ErrorCode;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;

/**
 * Errors of the post and feed endpoints → HTTP responses of the API contract.
 * Applies only to {@link PostController} and {@link FeedController}; it is consulted before
 * the common {@code shared.exception.GlobalExceptionHandler}.
 */
@RestControllerAdvice(assignableTypes = {PostController.class, FeedController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PostExceptionHandler {

    /**
     * An invalid request: a query parameter is missing ({@code followerId}), a path parameter is empty
     * after conversion ({@link MissingPathVariableException}: {@code postId} is a blank string),
     * a parameter is of a wrong type
     * (an id that is not a UUID, {@code limit} or {@code afterCreatedAt} that cannot be converted),
     * {@code limit} is out of the contract range ({@link HandlerMethodValidationException} of Spring MVC
     * or {@link ConstraintViolationException} of the {@code @Validated} interface), only one part
     * of the feed cursor is set.
     *
     * @return {@code 400} with the INVALID_POST_REQUEST error body
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            IncompleteFeedCursorException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest() {
        return ErrorCode.INVALID_POST_REQUEST.response();
    }

    /**
     * @return {@code 404} with the POST_NOT_FOUND error body
     */
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound() {
        return ErrorCode.POST_NOT_FOUND.response();
    }
}
