package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
     * A path id that is not a UUID, or invalid query parameters of the feed
     * ({@code MethodArgumentNotValidException} extends {@link BindException}).
     *
     * @return {@code 400} with the INVALID_POST_REQUEST error body
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, BindException.class})
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
