package ru.job4j.socialmediaapi.shared.security;

import java.util.UUID;

/**
 * Порт «кто выполняет запрос». Сценарии и контроллеры берут текущего пользователя только отсюда,
 * а не из тела или параметров запроса.
 * <p>
 * Сейчас реализован {@link HeaderCurrentUserProvider} (заголовок {@code X-User-Id});
 * после подключения Keycloak меняется только реализация ({@code sub} из JWT).
 */
public interface CurrentUserProvider {

    /**
     * @return id пользователя, который выполняет запрос
     * @throws UnauthenticatedException пользователь не определён
     */
    UUID currentUserId();
}
