package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("Persistence-адаптер дружбы: сохранение в таблицу friendships")
class FriendshipsPersistenceAdapterTest {

    /** Fixed time used for created rows. */
    private static final Instant NOW = Instant.parse("2026-10-06T10:15:00Z");

    /** Repository under test. */
    @Autowired
    private FriendshipsRepository repository;

    /** Creates the offers that friendships refer to. */
    @Autowired
    private FriendshipOfferRepository friendshipOfferRepository;

    @Test
    @DisplayName("Создание дружбы по принятой заявке возвращает сохранённые в БД данные")
    void whenCreateFriendshipThenReturnStoredData() {
        var users = orderedPair();
        var offerId = acceptedOffer(users[0], users[1]);
        var id = UUID.randomUUID();

        var result = repository.createFriendship(
                request(id, users[0], users[1], offerId)
        );

        var expected = new CreateFriendshipResponse(
                id, users[0], users[1], offerId, FriendshipStatus.ACTIVE, NOW
        );
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Повторное создание дружбы для той же пары выбросит FriendshipAlreadyExistsException")
    void whenCreateDuplicateFriendshipThenThrowException() {
        var users = orderedPair();
        var offerId = acceptedOffer(users[0], users[1]);
        repository.createFriendship(
                request(UUID.randomUUID(), users[0], users[1], offerId)
        );

        assertThatThrownBy(() -> repository.createFriendship(
                request(UUID.randomUUID(), users[0], users[1], offerId)
        )).isInstanceOf(FriendshipAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Дружбу с first_user_id > second_user_id отклонит CHECK в БД, адаптер её не переводит")
    void whenCreateFriendshipInNonCanonicalOrderThenThrowException() {
        var users = orderedPair();
        var offerId = acceptedOffer(users[0], users[1]);

        assertThatThrownBy(() -> repository.createFriendship(
                request(UUID.randomUUID(), users[1], users[0], offerId)
        )).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_friendships_canonical_order");
    }

    @Test
    @DisplayName("Дружбу со ссылкой на несуществующую заявку отклонит FK в БД, адаптер её не переводит")
    void whenCreateFriendshipWithUnknownOfferThenThrowException() {
        var users = orderedPair();

        assertThatThrownBy(() -> repository.createFriendship(
                request(UUID.randomUUID(), users[0], users[1], UUID.randomUUID())
        )).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_friendships_friendship_offer");
    }

    /**
     * Two random user ids ordered as PostgreSQL compares UUIDs (unsigned,
     * byte by byte), which matches the order of their string form;
     * {@link UUID#compareTo} compares signed values and may disagree.
     *
     * @return two user ids, the first less than the second
     */
    private static UUID[] orderedPair() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        return first.toString().compareTo(second.toString()) < 0
                ? new UUID[] {first, second}
                : new UUID[] {second, first};
    }

    private UUID acceptedOffer(UUID fromUserId, UUID toUserId) {
        var id = UUID.randomUUID();
        friendshipOfferRepository.createFriendshipOffer(
                new CreateFriendshipOfferRequest(
                        id, fromUserId, toUserId,
                        FriendshipOfferStatus.ACCEPTED, NOW, NOW
                )
        );
        return id;
    }

    private static CreateFriendshipRequest request(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID friendshipOfferId
    ) {
        return new CreateFriendshipRequest(
                id, firstUserId, secondUserId, friendshipOfferId,
                FriendshipStatus.ACTIVE, NOW
        );
    }
}
