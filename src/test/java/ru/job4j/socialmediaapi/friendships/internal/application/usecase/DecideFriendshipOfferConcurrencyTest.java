package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("Сценарии принятия и отклонения заявки: одновременные решения по одной заявке")
class DecideFriendshipOfferConcurrencyTest {

    /** Creates the offer to decide. */
    @Autowired
    private CreateFriendshipOffer createFriendshipOffer;

    /** Accept scenario under test, with real transactions. */
    @Autowired
    private AcceptFriendshipOffer acceptFriendshipOffer;

    /** Reject scenario under test, with real transactions. */
    @Autowired
    private RejectFriendshipOffer rejectFriendshipOffer;

    /** Direct access to the database to check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RepeatedTest(10)
    @DisplayName("accept и reject одновременно: один успех, второй — Already-исключение по победителю; "
            + "в БД статус победителя, дружба — только если победил accept")
    void whenAcceptAndRejectConcurrentlyThenExactlyOneWins() throws Exception {
        var sender = UUID.randomUUID();
        var recipient = UUID.randomUUID();
        var offerId = createFriendshipOffer.create(new CreateFriendshipOfferCommand(sender, recipient)).id();

        var outcome = runConcurrently(List.of(
                () -> acceptFriendshipOffer.accept(new AcceptFriendshipOfferCommand(offerId, recipient)),
                () -> rejectFriendshipOffer.reject(new RejectFriendshipOfferCommand(offerId, recipient))
        ));

        assertThat(outcome.results()).hasSize(1);
        var winner = outcome.results().getFirst().status();
        var expectedError = winner == FriendshipOfferStatus.ACCEPTED
                ? FriendshipOfferAlreadyAcceptedException.class
                : FriendshipOfferAlreadyRejectedException.class;
        assertThat(outcome.errors()).singleElement().isInstanceOf(expectedError);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM friendship_offer WHERE id = ?", String.class, offerId
        )).isEqualTo(winner.name());
        var accepted = winner == FriendshipOfferStatus.ACCEPTED ? 1 : 0;
        assertThat(friendshipsOf(offerId)).isEqualTo(accepted);
        assertThat(subscriptions(recipient, sender)).isEqualTo(accepted);
    }

    @RepeatedTest(10)
    @DisplayName("Два accept одновременно: один успех, второй — FriendshipOfferAlreadyAcceptedException; одна дружба")
    void whenTwoAcceptsConcurrentlyThenOneFriendship() throws Exception {
        var sender = UUID.randomUUID();
        var recipient = UUID.randomUUID();
        var offerId = createFriendshipOffer.create(new CreateFriendshipOfferCommand(sender, recipient)).id();
        Callable<FriendshipOfferDecisionResult> accept =
                () -> acceptFriendshipOffer.accept(new AcceptFriendshipOfferCommand(offerId, recipient));

        var outcome = runConcurrently(List.of(accept, accept));

        assertThat(outcome.results()).hasSize(1);
        assertThat(outcome.errors()).singleElement().isInstanceOf(FriendshipOfferAlreadyAcceptedException.class);
        assertThat(friendshipsOf(offerId)).isOne();
        assertThat(subscriptions(recipient, sender)).isOne();
    }

    /**
     * Starts the tasks at the same time and collects their results and errors.
     *
     * @param tasks decisions on the same offer
     * @return results of the successful tasks and errors of the failed ones
     */
    private Outcome runConcurrently(List<Callable<FriendshipOfferDecisionResult>> tasks) throws Exception {
        var start = new CountDownLatch(1);
        var results = new ArrayList<FriendshipOfferDecisionResult>();
        var errors = new ArrayList<Throwable>();
        // shutdownNow() instead of try-with-resources: close() would wait forever for a hung task
        var executor = Executors.newFixedThreadPool(tasks.size());
        try {
            var futures = new ArrayList<Future<FriendshipOfferDecisionResult>>();
            for (var task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            for (var future : futures) {
                try {
                    results.add(future.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    errors.add(e.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }
        return new Outcome(results, errors);
    }

    private int friendshipsOf(UUID offerId) {
        return count("SELECT count(*) FROM friendships WHERE friendship_offer_id = ?", offerId);
    }

    private int subscriptions(UUID followerId, UUID followedId) {
        return count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                followerId, followedId
        );
    }

    private int count(String sql, Object... args) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, args));
    }

    /**
     * Outcome of concurrent decisions.
     *
     * @param results results of the successful decisions
     * @param errors  errors of the failed decisions
     */
    private record Outcome(List<FriendshipOfferDecisionResult> results, List<Throwable> errors) {
    }
}
