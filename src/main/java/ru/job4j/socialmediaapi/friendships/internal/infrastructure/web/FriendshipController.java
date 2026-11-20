package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.GetFriends;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper.FriendshipWebMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Эндпоинт {@code GET /api/v1/friendships}: публичная страница друзей пользователя.
 * Путь, параметры и их валидация берутся из {@link FriendshipApi}, сгенерированного по OpenAPI-контракту
 * ({@code docs/api/paths/friends-get.yaml}); аннотации здесь не повторяются. На ошибки отвечает
 * {@code FriendshipOfferExceptionHandler}.
 */
@RestController
@RequiredArgsConstructor
public class FriendshipController implements FriendshipApi {

    /** Сценарий чтения страницы друзей. */
    private final GetFriends getFriends;

    /** Преобразует query-параметры в команду сценария и результат обратно. */
    private final FriendshipWebMapper mapper;

    /**
     * {@code 200} со страницей друзей {@code userId}, сначала новые дружбы.
     * Следующая страница запрашивается с {@code afterCreatedAt} и {@code afterId} из {@code next};
     * {@code next == null} — друзей больше нет. На некорректные параметры ({@code 400}) отвечает
     * {@code FriendshipOfferExceptionHandler}.
     *
     * @param userId         пользователь, чьи друзья читаются
     * @param limit          размер страницы, от 1 до 100 (20, если не задан)
     * @param afterCreatedAt {@code next.createdAt} предыдущей страницы
     * @param afterId        {@code next.id} предыдущей страницы
     * @return страница друзей
     */
    @Override
    public ResponseEntity<?> getFriends(UUID userId, Integer limit, Instant afterCreatedAt, UUID afterId) {
        FriendPageResult result = getFriends.get(mapper.toCommand(userId, limit, afterCreatedAt, afterId));
        return ResponseEntity.ok(mapper.toResponse(result));
    }
}
