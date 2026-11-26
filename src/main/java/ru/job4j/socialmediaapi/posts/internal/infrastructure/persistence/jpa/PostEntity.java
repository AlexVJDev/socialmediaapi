package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка таблицы {@code posts}: публикация автора. Логическое удаление меняет статус на {@code DELETED},
 * строка остаётся.
 *
 * <p>{@code id} задаёт домен, поэтому {@code @GeneratedValue} нет. Чтобы {@code save} новой публикации
 * сразу выполнял {@code INSERT} (а не {@code merge} с {@code SELECT} по id), сущность реализует
 * {@link Persistable}: созданная через {@link #newPost} — новая, загруженная или уже сохранённая — нет.
 * Сеттеров нет: изменение и удаление — запросами {@link PostJpaRepository}.
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostEntity implements Persistable<UUID> {

    /** Идентификатор публикации. */
    @Id
    private UUID id;

    /** Автор (UUID пользователя Keycloak, без внешнего ключа). */
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    /** Заголовок. */
    @Column(nullable = false)
    private String title;

    /** Текст публикации (колонка {@code text}). */
    @Column(nullable = false)
    private String content;

    /** Статус; хранится по имени, как в CHECK-ограничении таблицы. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostStatus status;

    /** Время создания. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Время последнего изменения. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** {@code true}, пока сущность не сохранена и не загружена из БД. */
    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity;

    /**
     * Конструктор со всеми колонками.
     *
     * @param id        id публикации
     * @param authorId  автор
     * @param title     заголовок
     * @param content   текст
     * @param status    статус
     * @param createdAt время создания
     * @param updatedAt время изменения
     * @param newEntity {@code true} — сущность ещё не сохранена
     */
    PostEntity(UUID id, UUID authorId, String title, String content, PostStatus status,
               Instant createdAt, Instant updatedAt, boolean newEntity) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.newEntity = newEntity;
    }

    /**
     * Новая, ещё не сохранённая публикация: {@code save} выполнит {@code INSERT}.
     *
     * @param id        id публикации (задаёт домен)
     * @param authorId  автор
     * @param title     заголовок
     * @param content   текст
     * @param status    статус
     * @param createdAt время создания
     * @param updatedAt время изменения
     * @return новая сущность
     */
    public static PostEntity newPost(UUID id, UUID authorId, String title, String content, PostStatus status,
                                     Instant createdAt, Instant updatedAt) {
        return new PostEntity(id, authorId, title, content, status, createdAt, updatedAt, true);
    }

    /**
     * @return {@code true}, если сущность ещё не сохранена: {@code save} вызовет {@code persist}
     */
    @Override
    public boolean isNew() {
        return newEntity;
    }

    /** После сохранения или загрузки из БД сущность больше не новая. */
    @PostPersist
    @PostLoad
    void markNotNew() {
        newEntity = false;
    }

    /**
     * Сущности равны, если у них один и тот же ненулевой {@code id}; id читается через геттер,
     * чтобы сравнение работало и для прокси Hibernate.
     *
     * @param other сравниваемый объект
     * @return {@code true}, если это та же публикация
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PostEntity entity)) {
            return false;
        }
        UUID currentId = getId();
        return currentId != null && currentId.equals(entity.getId());
    }

    /**
     * Хеш привязан к классу и не меняется после присвоения {@code id}.
     *
     * @return хеш класса сущности
     */
    @Override
    public final int hashCode() {
        return PostEntity.class.hashCode();
    }
}
