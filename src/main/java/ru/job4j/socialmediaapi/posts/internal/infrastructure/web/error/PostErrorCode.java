package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.error;

import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.shared.web.ErrorCode;

/**
 * Error codes of the post and feed endpoints: the constant name is the {@code code}
 * of the response body. The common {@code VALIDATION_ERROR} and {@code INTERNAL_ERROR}
 * are in {@link ru.job4j.socialmediaapi.shared.web.CommonErrorCode}.
 */
public enum PostErrorCode implements ErrorCode {

    /** There is no such post, or it is deleted. */
    POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Пост не найден"
    );

    /** HTTP status of the error response. */
    private final HttpStatus status;

    /** Human-readable {@code detail} of the error response body. */
    private final String detail;

    PostErrorCode(HttpStatus status, String detail) {
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
