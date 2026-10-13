package ru.job4j.socialmediaapi.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipResult;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("Сервис заявок в друзья: одновременное создание одной и той же заявки")
class OfferFriendshipServiceConcurrencyTest {

    private static final int THREADS = 2;

    @Autowired
    private CreateOfferFriendshipUseCase useCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RepeatedTest(10)
    @DisplayName("Два одновременных запроса: один Created, второй AlreadyPending, без дубликатов в БД")
    void whenCreateSameOfferConcurrentlyThenOneCreatedAndOneAlreadyPending() throws Exception {
        var command = new CreateOfferFriendshipCommand(UUID.randomUUID(), UUID.randomUUID());
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(THREADS);
        try {
            var futures = new ArrayList<Future<CreateOfferFriendshipResult>>();
            for (var i = 0; i < THREADS; i++) {
                Callable<CreateOfferFriendshipResult> task = () -> {
                    start.await();
                    return useCase.create(command);
                };
                futures.add(executor.submit(task));
            }
            start.countDown();

            var results = new ArrayList<CreateOfferFriendshipResult>();
            for (var future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(results)
                    .filteredOn(CreateOfferFriendshipResult.Created.class::isInstance)
                    .hasSize(1);
            assertThat(results)
                    .filteredOn(CreateOfferFriendshipResult.AlreadyPending.class::isInstance)
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }

        assertThat(count(
                "SELECT count(*) FROM offer_friendships WHERE from_user_id = ? AND to_user_id = ?",
                command.fromUserId(), command.toUserId()
        )).isOne();
        assertThat(count(
                "SELECT count(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                command.fromUserId(), command.toUserId()
        )).isOne();
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }
}
