package ru.job4j.socialmediaapi.posts.internal.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Доменная модель поста: изменение и удаление только автором")
class PostTest {

    /** Время создания поста. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:00:00Z");

    /** Время изменения или удаления. */
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    /** Автор поста. */
    private static final UUID AUTHOR = UUID.randomUUID();

    /** Опубликованный пост автора. */
    private final Post post = new Post(UUID.randomUUID(), AUTHOR, "Заголовок", "Текст",
            PostStatus.PUBLISHED, CREATED_AT, CREATED_AT);

    @Test
    @DisplayName("Автор меняет пост: новые заголовок, текст и updatedAt")
    void whenAuthorUpdatesThenNewFields() {
        assertThat(post.update(AUTHOR, "Новый", "Новый текст", NOW)).isEqualTo(new Post(
                post.id(), AUTHOR, "Новый", "Новый текст", PostStatus.PUBLISHED, CREATED_AT, NOW));
    }

    @Test
    @DisplayName("Автор удаляет пост: статус DELETED")
    void whenAuthorDeletesThenDeleted() {
        var deleted = post.delete(AUTHOR, NOW);

        assertThat(deleted.status()).isEqualTo(PostStatus.DELETED);
        assertThat(deleted.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Чужой пост нельзя изменить или удалить: PostNotFoundException")
    void whenOtherUserThenNotFound() {
        var other = UUID.randomUUID();

        assertThatThrownBy(() -> post.update(other, "Новый", "Новый текст", NOW))
                .isInstanceOf(PostNotFoundException.class);
        assertThatThrownBy(() -> post.delete(other, NOW))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    @DisplayName("Удалённый пост нельзя удалить повторно: PostNotFoundException")
    void whenDeletedThenNotFound() {
        var deleted = post.delete(AUTHOR, NOW);

        assertThatThrownBy(() -> deleted.delete(AUTHOR, NOW))
                .isInstanceOf(PostNotFoundException.class);
    }
}
