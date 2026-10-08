package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.application.port.out.FriendshipRepository;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FriendshipRepositoryJdbcTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:15:00Z");

    @Autowired
    private FriendshipRepository repository;

    @Autowired
    private OfferFriendshipRepository offerFriendshipRepository;

    @Test
    void whenCreateFriendshipThenReturnStoredData() {
        var users = orderedPair();
        var offerId = acceptedOffer(users[0], users[1]);
        var id = UUID.randomUUID();

        var result = repository.createFriendship(
                request(id, users[0], users[1], offerId)
        );

        var expected = new FriendshipRepository.CreateFriendshipResponse(
                id, users[0], users[1], offerId, NOW
        );
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void whenCreateDuplicateFriendshipThenThrowException() {
        var users = orderedPair();
        var offerId = acceptedOffer(users[0], users[1]);
        repository.createFriendship(
                request(UUID.randomUUID(), users[0], users[1], offerId)
        );

        assertThatThrownBy(() -> repository.createFriendship(
                request(UUID.randomUUID(), users[0], users[1], offerId)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenCreateFriendshipInNonCanonicalOrderThenThrowException() {
        var users = orderedPair();
        var offerId = acceptedOffer(users[0], users[1]);

        assertThatThrownBy(() -> repository.createFriendship(
                request(UUID.randomUUID(), users[1], users[0], offerId)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenCreateFriendshipWithUnknownOfferThenThrowException() {
        var users = orderedPair();

        assertThatThrownBy(() -> repository.createFriendship(
                request(UUID.randomUUID(), users[0], users[1], UUID.randomUUID())
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Two random user ids ordered as PostgreSQL compares UUIDs (unsigned,
     * byte by byte), which matches the order of their string form;
     * {@link UUID#compareTo} compares signed values and may disagree.
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
        offerFriendshipRepository.createOfferFriendship(
                new OfferFriendshipRepository.CreateOfferFriendshipRequest(
                        id, fromUserId, toUserId,
                        OfferFriendshipStatus.ACCEPTED, NOW, NOW
                )
        );
        return id;
    }

    private static FriendshipRepository.CreateFriendshipRequest request(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID offerFriendshipId
    ) {
        return new FriendshipRepository.CreateFriendshipRequest(
                id, firstUserId, secondUserId, offerFriendshipId, NOW
        );
    }
}
