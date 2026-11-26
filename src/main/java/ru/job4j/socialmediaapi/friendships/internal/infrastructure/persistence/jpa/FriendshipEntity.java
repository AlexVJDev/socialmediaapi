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
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка таблицы {@code friendships}: дружба двух пользователей в каноническом порядке
 * ({@code first_user_id < second_user_id}).
 *
 * <p>Ссылки на пользователей и на заявку, из которой появилась дружба, хранятся как {@code UUID},
 * без связей JPA. {@code id} задаёт домен, поэтому {@code @GeneratedValue} нет. Сеттеров нет:
 * строка пишется запросами {@link FriendshipJpaRepository}.
 */
@Entity
@Table(name = "friendships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FriendshipEntity {

    /** Идентификатор дружбы. */
    @Id
    private UUID id;

    /** Меньший из двух UUID участников. */
    @Column(name = "first_user_id", nullable = false)
    private UUID firstUserId;

    /** Больший из двух UUID участников. */
    @Column(name = "second_user_id", nullable = false)
    private UUID secondUserId;

    /** Заявка, после принятия которой появилась дружба. */
    @Column(name = "friendship_offer_id", nullable = false)
    private UUID friendshipOfferId;

    /** Статус дружбы; хранится по имени, как в CHECK-ограничении таблицы. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendshipStatus status;

    /** Время создания дружбы. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Сущности равны, если у них один и тот же ненулевой {@code id}; id читается через геттер,
     * чтобы сравнение работало и для прокси Hibernate.
     *
     * @param other сравниваемый объект
     * @return {@code true}, если это та же дружба
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FriendshipEntity entity)) {
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
        return FriendshipEntity.class.hashCode();
    }
}
