package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;

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
@DisplayName("Сценарий создания заявки в друзья: одновременное создание заявок одной пары")
class CreateFriendshipOfferConcurrencyTest {

    /** Number of concurrent requests for the same pair of users. */
    private static final int THREADS = 2;

    /** Use case under test, with real transactions. */
    @Autowired
    private CreateFriendshipOffer useCase;

    /** Direct access to the database to prepare and check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RepeatedTest(10)
    @DisplayName("Два одновременных одинаковых запроса: один создаёт заявку, второй получает "
            + "FriendshipOfferAlreadyPendingException, без дубликатов в БД")
    void whenCreateSameOfferConcurrentlyThenOneCreatedAndOneAlreadyPending() throws Exception {
        var command = new CreateFriendshipOfferCommand(UUID.randomUUID(), UUID.randomUUID());

        assertOneCreatedAndOneAlreadyPending(command, command);

        assertThat(count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                command.fromUserId(), command.toUserId()
        )).isOne();
    }

    @RepeatedTest(10)
    @DisplayName("Две одновременные встречные заявки: одна создаётся, вторая получает "
            + "FriendshipOfferAlreadyPendingException, в БД одна заявка пары")
    void whenCreateCounterOffersConcurrentlyThenOneCreatedAndOneAlreadyPending() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();

        var created = assertOneCreatedAndOneAlreadyPending(
                new CreateFriendshipOfferCommand(aliceId, borisId),
                new CreateFriendshipOfferCommand(borisId, aliceId)
        );

        assertThat(count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                created.fromUserId(), created.toUserId()
        )).isOne();
        assertThat(count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                created.toUserId(), created.fromUserId()
        )).isZero();
    }

    /**
     * Runs the two commands at the same time and checks that exactly one offer of the pair
     * is created and the other command fails with {@link FriendshipOfferAlreadyPendingException}.
     *
     * @param first  first command
     * @param second second command, for the same pair of users
     * @return the created offer
     */
    private CreateFriendshipOfferResult assertOneCreatedAndOneAlreadyPending(
            CreateFriendshipOfferCommand first, CreateFriendshipOfferCommand second
    ) throws Exception {
        var start = new CountDownLatch(1);
        var results = new ArrayList<CreateFriendshipOfferResult>();
        var errors = new ArrayList<Throwable>();
        // shutdownNow() instead of try-with-resources: close() would wait forever for a hung task
        var executor = Executors.newFixedThreadPool(THREADS);
        try {
            var futures = new ArrayList<Future<CreateFriendshipOfferResult>>();
            for (var command : List.of(first, second)) {
                Callable<CreateFriendshipOfferResult> task = () -> {
                    start.await();
                    return useCase.create(command);
                };
                futures.add(executor.submit(task));
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

        assertThat(results).hasSize(1);
        assertThat(errors).singleElement().isInstanceOf(FriendshipOfferAlreadyPendingException.class);
        assertThat(count("""
                SELECT count(*) FROM friendship_offer
                WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)
                """, first.fromUserId(), first.toUserId(), first.toUserId(), first.fromUserId()
        )).isOne();
        return results.getFirst();
    }

    private int count(String sql, Object... args) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, args));
    }
}
