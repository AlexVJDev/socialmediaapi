package ru.job4j.socialmediaapi.posts.internal.domain.model;

import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;

import java.time.Instant;
import java.util.UUID;

/**
 * Пост. Изменить или удалить его может только автор и только опубликованный пост.
 * Чужой или удалённый пост для этих действий считается отсутствующим ({@link PostNotFoundException}),
 * чтобы не раскрывать, что пост существует.
 * <p>
 * Неизменяемый: {@link #update} и {@link #delete} возвращают новый экземпляр.
 *
 * @param id        идентификатор поста
 * @param authorId  автор поста
 * @param title     заголовок
 * @param content   текст
 * @param status    статус
 * @param createdAt время создания
 * @param updatedAt время последнего изменения
 */
public record Post(
        UUID id,
        UUID authorId,
        String title,
        String content,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Меняет заголовок и текст.
     *
     * @param actorId пользователь, который меняет пост
     * @param title   новый заголовок
     * @param content новый текст
     * @param now     время изменения
     * @return пост с новыми заголовком, текстом и {@code updatedAt}
     * @throws PostNotFoundException {@code actorId} не автор или пост удалён
     */
    public Post update(UUID actorId, String title, String content, Instant now) {
        requireEditableBy(actorId);
        return new Post(id, authorId, title, content, status, createdAt, now);
    }

    /**
     * Удаляет пост логически: статус становится {@link PostStatus#DELETED}.
     *
     * @param actorId пользователь, который удаляет пост
     * @param now     время удаления
     * @return удалённый пост
     * @throws PostNotFoundException {@code actorId} не автор или пост уже удалён
     */
    public Post delete(UUID actorId, Instant now) {
        requireEditableBy(actorId);
        return new Post(id, authorId, title, content, PostStatus.DELETED, createdAt, now);
    }

    /**
     * @param actorId пользователь, который меняет или удаляет пост
     * @throws PostNotFoundException {@code actorId} не автор или пост не опубликован
     */
    private void requireEditableBy(UUID actorId) {
        if (!authorId.equals(actorId) || status != PostStatus.PUBLISHED) {
            throw new PostNotFoundException(id);
        }
    }
}
