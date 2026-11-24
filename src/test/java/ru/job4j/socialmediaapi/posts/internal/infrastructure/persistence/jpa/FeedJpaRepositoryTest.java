package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.jpa.SubscriptionJpaRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("JPA-репозиторий ленты: посты авторов, на которых подписан читатель")
class FeedJpaRepositoryTest {

    /** Проверяемый репозиторий ленты. */
    @Autowired
    private FeedJpaRepository feed;

    /** Публикации: сохранение и логическое удаление. */
    @Autowired
    private PostJpaRepository posts;

    /** Подписки — через репозиторий модуля subscriptions (только в тесте, Р6-4). */
    @Autowired
    private SubscriptionJpaRepository subscriptions;

    @Test
    @DisplayName("Лента — только опубликованные посты автора, на которого подписан читатель; "
            + "при одинаковом времени — по id по убыванию")
    void whenSubscribedThenFeedHasOnlyPublishedPostsOfFollowedAuthorsOrderedByIdDesc() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        var now = Instant.parse("2026-09-22T10:15:00Z");
        subscriptions.create(UUID.randomUUID(), reader, author, now);

        var first = publish(author, now);
        var second = publish(author, now);
        var deleted = publish(author, now.plusSeconds(60));
        posts.deletePublished(deleted, author, now.plusSeconds(120));
        publish(stranger, now.plusSeconds(180));

        // Порядок id в PostgreSQL (uuid сравнивается побайтно без знака) совпадает с порядком строк UUID.
        List<UUID> expected = Stream.of(first, second)
                .sorted(Comparator.comparing(UUID::toString).reversed())
                .toList();

        assertThat(feed.findFeedFirstPage(reader, 10))
                .extracting(PostEntity::getId)
                .containsExactlyElementsOf(expected);
    }

    /**
     * @param authorId  автор
     * @param createdAt время создания
     * @return id новой опубликованной публикации
     */
    private UUID publish(UUID authorId, Instant createdAt) {
        var id = UUID.randomUUID();
        posts.save(PostEntity.newPost(id, authorId, "Заголовок", "Текст", PostStatus.PUBLISHED, createdAt, createdAt));
        return id;
    }
}
