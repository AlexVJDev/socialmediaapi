package ru.job4j.socialmediaapi.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Error code of the API: the constant name of an implementing enum is the {@code code} of the response body.
 * Common codes are in {@link CommonErrorCode}, the codes of a module are in its {@code <Module>ErrorCode}
 * ({@code <module>.internal.infrastructure.web.error}).
 */
public interface ErrorCode {

    /**
     * @return machine-readable code of the error; implemented by the enum constant name
     */
    String name();

    /**
     * @return HTTP status of the error response
     */
    HttpStatus status();

    /**
     * @return human-readable {@code detail} of the error response body
     */
    String detail();

    /**
     * @return the response with this error's status and {@code {status, detail, code}} body
     *         of type {@code application/problem+json}
     */
    default ResponseEntity<ApiErrorResponse> response() {
        return ResponseEntity.status(status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ApiErrorResponse()
                        .status(status().value())
                        .detail(detail())
                        .code(name()));
    }
}
