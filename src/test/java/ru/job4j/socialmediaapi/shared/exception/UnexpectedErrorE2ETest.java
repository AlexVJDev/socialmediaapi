package ru.job4j.socialmediaapi.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.job4j.socialmediaapi.E2ETest;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.GetFriendshipOffer;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * E2E of an unexpected error: a use case fails with an exception that no module handles,
 * {@link GlobalExceptionHandler} answers {@code 500 INTERNAL_ERROR}. A separate class because
 * {@link MockitoBean} changes the Spring context, which the other E2E tests share.
 */
@E2ETest
@DisplayName("E2E: непредвиденная ошибка — 500 INTERNAL_ERROR")
class UnexpectedErrorE2ETest {

    /** HTTP client bound to the random port of the running application. */
    @Autowired
    private TestRestTemplate restTemplate;

    /** Use case that fails unexpectedly. */
    @MockitoBean
    private GetFriendshipOffer getFriendshipOffer;

    @Test
    @DisplayName("Исключение в use case: 500 INTERNAL_ERROR, текст исключения в тело не попадает")
    void whenUseCaseFailsThenInternalErrorWithoutDetails() {
        when(getFriendshipOffer.get(any())).thenThrow(new IllegalStateException("connection to secret-db refused"));

        var response = restTemplate.getForEntity(
                "/api/v1/friendship-offer/{offerId}", ApiErrorResponse.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .isEqualTo(new ApiErrorResponse(500, "Внутренняя ошибка сервера", "INTERNAL_ERROR"));
    }
}
