package ru.job4j.socialmediaapi.friendships.internal.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.application.service.CreateFriendshipOfferUseCase.CreateFriendshipOfferUseCaseRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.service.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;

import java.util.ArrayList;
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
@DisplayName("Сервис заявок в друзья: одновременное создание одной и той же заявки")
class FriendshipOfferServiceConcurrencyTest {

    /** Number of concurrent requests for the same offer. */
    private static final int THREADS = 2;

    /** Use case under test, with real transactions. */
    @Autowired
    private CreateFriendshipOfferUseCase useCase;

    /** Direct access to the database to prepare and check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RepeatedTest(10)
    @DisplayName("Два одновременных запроса: один создаёт заявку, второй получает "
            + "FriendshipOfferAlreadyPendingException, без дубликатов в БД")
    void whenCreateSameOfferConcurrentlyThenOneCreatedAndOneAlreadyPending() throws Exception {
        var useCaseRequest = new CreateFriendshipOfferUseCaseRequest(UUID.randomUUID(), UUID.randomUUID());
        var start = new CountDownLatch(1);
        // shutdownNow() instead of try-with-resources: close() would wait forever for a hung task
        var executor = Executors.newFixedThreadPool(THREADS);
        try {
            var futures = new ArrayList<Future<CreateFriendshipOfferResult>>();
            for (var i = 0; i < THREADS; i++) {
                Callable<CreateFriendshipOfferResult> task = () -> {
                    start.await();
                    return useCase.create(useCaseRequest);
                };
                futures.add(executor.submit(task));
            }
            start.countDown();

            var results = new ArrayList<CreateFriendshipOfferResult>();
            var errors = new ArrayList<Throwable>();
            for (var future : futures) {
                try {
                    results.add(future.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    errors.add(e.getCause());
                }
            }

            assertThat(results).hasSize(1);
            assertThat(errors).singleElement().isInstanceOf(FriendshipOfferAlreadyPendingException.class);
        } finally {
            executor.shutdownNow();
        }

        assertThat(count(
                "SELECT count(*) FROM friendship_offer WHERE from_user_id = ? AND to_user_id = ?",
                useCaseRequest.fromUserId(), useCaseRequest.toUserId()
        )).isOne();
        assertThat(count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                useCaseRequest.fromUserId(), useCaseRequest.toUserId()
        )).isOne();
    }

    private int count(String sql, Object... args) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, args));
    }
}
