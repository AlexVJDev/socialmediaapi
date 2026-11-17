package ru.job4j.socialmediaapi.shared.security;

/**
 * Пользователь, выполняющий запрос, не определён: ответ {@code 401 UNAUTHORIZED}.
 * Сообщение не содержит значения, присланного клиентом.
 */
public class UnauthenticatedException extends RuntimeException {

    /**
     * @param message причина, без данных клиента
     */
    public UnauthenticatedException(String message) {
        super(message);
    }
}
