package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository.UpdatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("JDBC-репозиторий публикаций: создание, чтение, изменение и логическое удаление в таблице posts")
class PostRepositoryJdbcTest {

    /** Fixed creation time of posts. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:15:00Z");

    /** Fixed time of a later change or deletion. */
    private static final Instant CHANGED_AT = Instant.parse("2026-10-15T12:30:00Z");

    /** Repository under test. */
    @Autowired
    private PostRepository repository;

    /** Direct access to the table to check stored data and constraints. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("createPost возвращает сохранённые в БД данные со статусом PUBLISHED")
    void whenCreatePostThenReturnStoredData() {
        var request = request(UUID.randomUUID());

        var result = repository.createPost(request);

        assertThat(result).isEqualTo(new PostResponse(
                request.id(), request.authorId(), "Заголовок", "Текст",
                PostStatus.PUBLISHED, CREATED_AT, CREATED_AT
        ));
        assertThat(storedPost(request.id())).isEqualTo(result);
    }

    @Test
    @DisplayName("findPublishedById после создания возвращает ту же публикацию")
    void whenFindCreatedPostThenReturnIt() {
        var created = repository.createPost(request(UUID.randomUUID()));

        assertThat(repository.findPublishedById(created.id())).contains(created);
    }

    @Test
    @DisplayName("findPublishedById несуществующей публикации возвращает пустой результат")
    void whenFindUnknownPostThenEmpty() {
        assertThat(repository.findPublishedById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("findPublishedById удалённой публикации возвращает пустой результат, хотя строка в БД осталась")
    void whenFindDeletedPostThenEmpty() {
        var created = repository.createPost(request(UUID.randomUUID()));
        repository.deletePost(new DeletePostRequest(created.id(), created.authorId(), CHANGED_AT));

        assertThat(repository.findPublishedById(created.id())).isEmpty();
        assertThat(storedPost(created.id()).status()).isEqualTo(PostStatus.DELETED);
    }

    @Test
    @DisplayName("Автор меняет заголовок и текст: новые значения и updated_at, created_at и автор не меняются")
    void whenAuthorUpdatesPostThenNewTitleAndContent() {
        var created = repository.createPost(request(UUID.randomUUID()));

        var result = repository.updatePost(new UpdatePostRequest(
                created.id(), created.authorId(), "Новый заголовок", "Новый текст", CHANGED_AT
        ));

        var expected = new PostResponse(
                created.id(), created.authorId(), "Новый заголовок", "Новый текст",
                PostStatus.PUBLISHED, CREATED_AT, CHANGED_AT
        );
        assertThat(result).contains(expected);
        assertThat(storedPost(created.id())).isEqualTo(expected);
    }

    @Test
    @DisplayName("Не автор не может изменить публикацию: пустой результат, данные в БД не изменились")
    void whenOtherUserUpdatesPostThenEmptyAndPostUnchanged() {
        var created = repository.createPost(request(UUID.randomUUID()));

        var result = repository.updatePost(new UpdatePostRequest(
                created.id(), UUID.randomUUID(), "Чужой заголовок", "Чужой текст", CHANGED_AT
        ));

        assertThat(result).isEmpty();
        assertThat(storedPost(created.id())).isEqualTo(created);
    }

    @Test
    @DisplayName("Удалённую публикацию нельзя изменить: пустой результат, данные в БД не изменились")
    void whenUpdateDeletedPostThenEmptyAndPostUnchanged() {
        var created = repository.createPost(request(UUID.randomUUID()));
        repository.deletePost(new DeletePostRequest(created.id(), created.authorId(), CHANGED_AT));
        var deleted = storedPost(created.id());

        var result = repository.updatePost(new UpdatePostRequest(
                created.id(), created.authorId(), "Новый заголовок", "Новый текст", CHANGED_AT.plusSeconds(60)
        ));

        assertThat(result).isEmpty();
        assertThat(storedPost(created.id())).isEqualTo(deleted);
    }

    @Test
    @DisplayName("Изменение несуществующей публикации возвращает пустой результат")
    void whenUpdateUnknownPostThenEmpty() {
        var result = repository.updatePost(new UpdatePostRequest(
                UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст", CHANGED_AT
        ));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Автор удаляет публикацию: true, строка осталась со статусом DELETED и новым updated_at")
    void whenAuthorDeletesPostThenStatusDeleted() {
        var created = repository.createPost(request(UUID.randomUUID()));

        var deleted = repository.deletePost(new DeletePostRequest(created.id(), created.authorId(), CHANGED_AT));

        assertThat(deleted).isTrue();
        assertThat(storedPost(created.id())).isEqualTo(new PostResponse(
                created.id(), created.authorId(), "Заголовок", "Текст",
                PostStatus.DELETED, CREATED_AT, CHANGED_AT
        ));
    }

    @Test
    @DisplayName("Не автор не может удалить публикацию: false, статус остался PUBLISHED")
    void whenOtherUserDeletesPostThenFalseAndPostUnchanged() {
        var created = repository.createPost(request(UUID.randomUUID()));

        var deleted = repository.deletePost(new DeletePostRequest(created.id(), UUID.randomUUID(), CHANGED_AT));

        assertThat(deleted).isFalse();
        assertThat(storedPost(created.id())).isEqualTo(created);
    }

    @Test
    @DisplayName("Повторное удаление: false, данные первого удаления не меняются")
    void whenDeletePostAgainThenFalseAndPostUnchanged() {
        var created = repository.createPost(request(UUID.randomUUID()));
        repository.deletePost(new DeletePostRequest(created.id(), created.authorId(), CHANGED_AT));
        var deleted = storedPost(created.id());

        var deletedAgain = repository.deletePost(
                new DeletePostRequest(created.id(), created.authorId(), CHANGED_AT.plusSeconds(60))
        );

        assertThat(deletedAgain).isFalse();
        assertThat(storedPost(created.id())).isEqualTo(deleted);
    }

    @Test
    @DisplayName("Статус вне PUBLISHED|DELETED отклоняется ограничением chk_posts_status")
    void whenInsertUnknownStatusThenThrowException() {
        assertThatThrownBy(() -> insert("Заголовок", "DRAFT"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_posts_status");
    }

    @Test
    @DisplayName("Публикация без заголовка отклоняется ограничением NOT NULL")
    void whenInsertNullTitleThenThrowException() {
        assertThatThrownBy(() -> insert(null, "PUBLISHED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("title");
    }

    private static CreatePostRequest request(UUID authorId) {
        return new CreatePostRequest(
                UUID.randomUUID(), authorId, "Заголовок", "Текст",
                PostStatus.PUBLISHED, CREATED_AT, CREATED_AT
        );
    }

    private void insert(String title, String status) {
        jdbcTemplate.update(
                "INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), title, "Текст", status,
                Timestamp.from(CREATED_AT), Timestamp.from(CREATED_AT)
        );
    }

    /**
     * Reads the post straight from the table, whatever its status.
     *
     * @param id id of the post
     * @return the stored post
     */
    private PostResponse storedPost(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT id, author_id, title, content, status, created_at, updated_at FROM posts WHERE id = ?",
                (resultSet, rowNum) -> new PostResponse(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("author_id", UUID.class),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        PostStatus.valueOf(resultSet.getString("status")),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant()
                ),
                id
        );
    }
}
