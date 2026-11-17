package ru.job4j.socialmediaapi.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Временная реализация {@link CurrentUserProvider} до подключения Keycloak: id пользователя берётся
 * из заголовка {@value #HEADER} текущего HTTP-запроса.
 * <p>
 * Запрос читается через {@link RequestContextHolder}, так как бин — singleton.
 */
@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    /** Заголовок с id (UUID) пользователя, который выполняет запрос. */
    public static final String HEADER = "X-User-Id";

    /**
     * @return id из заголовка {@value #HEADER}
     * @throws UnauthenticatedException заголовка нет, он пуст, не UUID или UUID не в каноническом виде
     *                                  (36 символов с дефисами, например {@code 1-1-1-1-1} отклоняется)
     */
    @Override
    public UUID currentUserId() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String value = request.getHeader(HEADER);
        if (value == null || value.isBlank()) {
            throw new UnauthenticatedException("Header " + HEADER + " is missing");
        }
        String trimmed = value.strip();
        UUID userId;
        try {
            userId = UUID.fromString(trimmed);
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException("Header " + HEADER + " is not a UUID");
        }
        if (!userId.toString().equalsIgnoreCase(trimmed)) {
            throw new UnauthenticatedException("Header " + HEADER + " is not a canonical UUID");
        }
        return userId;
    }
}
