package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@DisplayName("JPA-репозиторий публикаций: таблица posts")
class PostJpaRepositoryTest extends PostgreSqlIntegrationTest {

    /** Проверяемый репозиторий. */
    @Autowired
    private PostJpaRepository repository;

    @Test
    @DisplayName("Сохранённая публикация находится как PUBLISHED; после логического удаления — нет, строка DELETED")
    void whenSaveThenFoundAsPublishedAndAfterDeleteNotFound() {
        var id = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var now = Instant.parse("2026-09-22T10:15:00Z");
        var deletedAt = now.plusSeconds(60);
        var expected = new PostEntity(id, authorId, "Заголовок", "Текст", PostStatus.PUBLISHED, now, now, false);

        repository.save(PostEntity.newPost(id, authorId, "Заголовок", "Текст", PostStatus.PUBLISHED, now, now));

        assertThat(repository.findByIdAndStatus(id, PostStatus.PUBLISHED).orElseThrow())
                .usingRecursiveComparison()
                .ignoringFields("newEntity")
                .isEqualTo(expected);

        assertThat(repository.deletePublished(id, authorId, deletedAt)).isEqualTo(1);

        assertThat(repository.findByIdAndStatus(id, PostStatus.PUBLISHED)).isEmpty();
        var deleted = repository.findById(id).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(deleted.getUpdatedAt()).isEqualTo(deletedAt);
    }
}
