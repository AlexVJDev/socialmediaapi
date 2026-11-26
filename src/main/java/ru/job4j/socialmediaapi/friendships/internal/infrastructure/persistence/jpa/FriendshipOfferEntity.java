package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка таблицы {@code friendship_offer}: заявка в друзья.
 *
 * <p>Таблицу и ограничения создаёт Liquibase, Hibernate только сверяет отображение со схемой.
 * {@code id} задаёт домен ({@code FriendshipOffer.create}), поэтому {@code @GeneratedValue} нет.
 * Сеттеров нет: строка пишется запросами {@link FriendshipOfferJpaRepository}.
 */
@Entity
@Table(name = "friendship_offer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FriendshipOfferEntity {

    /** Идентификатор заявки. */
    @Id
    private UUID id;

    /** Отправитель заявки (UUID пользователя Keycloak). */
    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    /** Получатель заявки (UUID пользователя Keycloak). */
    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    /** Статус заявки; хранится по имени, как в CHECK-ограничении таблицы. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendshipOfferStatus status;

    /** Время создания заявки. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Время последнего изменения статуса. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Сущности равны, если у них один и тот же ненулевой {@code id}; id читается через геттер,
     * чтобы сравнение работало и для прокси Hibernate.
     *
     * @param other сравниваемый объект
     * @return {@code true}, если это та же заявка
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FriendshipOfferEntity entity)) {
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
        return FriendshipOfferEntity.class.hashCode();
    }
}
