package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa;

import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Создаёт {@link PostEntity} в состоянии «загружена из БД» для тестов других пакетов:
 * конструктор сущности со всеми полями package-private.
 */
public final class PostEntityFixtures {

    private PostEntityFixtures() {
    }

    /**
     * @param id        id публикации
     * @param authorId  автор
     * @param title     заголовок
     * @param content   текст
     * @param status    статус
     * @param createdAt время создания
     * @param updatedAt время изменения
     * @return сохранённая (не новая) сущность с заданными полями
     */
    public static PostEntity post(
            UUID id, UUID authorId, String title, String content,
            PostStatus status, Instant createdAt, Instant updatedAt
    ) {
        return new PostEntity(id, authorId, title, content, status, createdAt, updatedAt, false);
    }
}
