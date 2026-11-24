package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка таблицы {@code subscriptions}: направленная подписка {@code follower_id → followed_id}.
 *
 * <p>Ссылки на пользователей хранятся как {@code UUID} (учётные записи — в Keycloak). {@code id} задаёт
 * сценарий, поэтому {@code @GeneratedValue} нет. Сеттеров нет: строка пишется запросами
 * {@link SubscriptionJpaRepository}.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SubscriptionEntity {

    /** Идентификатор подписки. */
    @Id
    private UUID id;

    /** Подписчик. */
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    /** Пользователь, на которого подписались. */
    @Column(name = "followed_id", nullable = false)
    private UUID followedId;

    /** Время создания подписки. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Сущности равны, если у них один и тот же ненулевой {@code id}; id читается через геттер,
     * чтобы сравнение работало и для прокси Hibernate.
     *
     * @param other сравниваемый объект
     * @return {@code true}, если это та же подписка
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SubscriptionEntity entity)) {
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
        return SubscriptionEntity.class.hashCode();
    }
}
