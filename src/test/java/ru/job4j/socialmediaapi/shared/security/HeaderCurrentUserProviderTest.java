package ru.job4j.socialmediaapi.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Текущий пользователь из заголовка X-User-Id")
class HeaderCurrentUserProviderTest {

    /** Проверяемая реализация. */
    private final HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider();

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("UUID в заголовке — id текущего пользователя")
    void whenHeaderIsUuidThenReturnsIt() {
        var userId = UUID.randomUUID();
        bindRequest(userId.toString());

        assertThat(provider.currentUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Нет заголовка — UnauthenticatedException")
    void whenNoHeaderThenUnauthenticated() {
        bindRequest(null);

        assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthenticatedException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"abc", "1-1-1-1-1"})
    @DisplayName("Заголовок не UUID или UUID не в каноническом виде — UnauthenticatedException")
    void whenHeaderIsNotCanonicalUuidThenUnauthenticated(String header) {
        bindRequest(header);

        assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthenticatedException.class);
    }

    private static void bindRequest(String header) {
        var request = new MockHttpServletRequest();
        if (header != null) {
            request.addHeader(HeaderCurrentUserProvider.HEADER, header);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
