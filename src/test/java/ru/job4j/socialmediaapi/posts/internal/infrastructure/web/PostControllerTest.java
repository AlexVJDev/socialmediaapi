package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Posts and subscriptions are prepared through the posts port and the subscriptions module API;
 * each test uses new user ids, since the container is shared between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("GET /api/v1/posts/{postId} и GET /api/v1/feed: чтение постов и ленты")
class PostControllerTest {

    /** Base time of posts; later posts are created minutes after it. */
    private static final Instant BASE_TIME = Instant.parse("2026-10-15T10:00:00Z");

    /** MockMvc to call the API without a running server. */
    @Autowired
    private MockMvc mockMvc;

    /** Creates and deletes posts. */
    @Autowired
    private PostRepository postRepository;

    /** Subscribes readers to authors (API of the subscriptions module). */
    @Autowired
    private Subscriptions subscriptions;

    @Test
    @DisplayName("Опубликованный пост: 200 и все поля поста")
    void whenPostPublishedThenOk() throws Exception {
        var author = UUID.randomUUID();
        var postId = post(author, 0);

        mockMvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()))
                .andExpect(jsonPath("$.authorId").value(author.toString()))
                .andExpect(jsonPath("$.title").value("Пост 0"))
                .andExpect(jsonPath("$.content").value("Текст 0"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-15T10:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-15T10:00:00Z"));
    }

    @Test
    @DisplayName("Несуществующий пост: 404 POST_NOT_FOUND")
    void whenPostMissingThenNotFound() throws Exception {
        expectError(mockMvc.perform(get("/api/v1/posts/{id}", UUID.randomUUID())), 404, "POST_NOT_FOUND");
    }

    @Test
    @DisplayName("Удалённый пост: 404 POST_NOT_FOUND")
    void whenPostDeletedThenNotFound() throws Exception {
        var author = UUID.randomUUID();
        var postId = post(author, 0);
        postRepository.deletePost(new DeletePostRequest(postId, author, BASE_TIME.plusSeconds(60)));

        expectError(mockMvc.perform(get("/api/v1/posts/{id}", postId)), 404, "POST_NOT_FOUND");
    }

    @Test
    @DisplayName("Id поста не UUID: 400 INVALID_POST_REQUEST")
    void whenPostIdNotUuidThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get("/api/v1/posts/42")), 400, "INVALID_POST_REQUEST");
    }

    @Test
    @DisplayName("Лента: посты авторов из подписок, от новых к старым, без чужих постов")
    void whenFeedThenPostsOfFollowedAuthorsNewestFirst() throws Exception {
        var reader = UUID.randomUUID();
        var alice = UUID.randomUUID();
        var bob = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        var first = post(alice, 0);
        var second = post(bob, 1);
        var third = post(alice, 2);
        post(stranger, 3);
        subscriptions.subscribeIfAbsent(reader, alice, BASE_TIME);
        subscriptions.subscribeIfAbsent(reader, bob, BASE_TIME);

        mockMvc.perform(get("/api/v1/feed").param("followerId", reader.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(contains(
                        third.toString(), second.toString(), first.toString())))
                .andExpect(jsonPath("$.posts[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    @DisplayName("Лента по страницам: next ведёт на следующую страницу, на последней next = null")
    void whenFeedPagedThenNextLeadsToNextPage() throws Exception {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var first = post(author, 0);
        var second = post(author, 1);
        var third = post(author, 2);
        subscriptions.subscribeIfAbsent(reader, author, BASE_TIME);

        var page1 = mockMvc.perform(get("/api/v1/feed")
                        .param("followerId", reader.toString())
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(contains(third.toString(), second.toString())))
                .andExpect(jsonPath("$.next.id").value(second.toString()))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/feed")
                        .param("followerId", reader.toString())
                        .param("limit", "2")
                        .param("afterCreatedAt", JsonPath.<String>read(page1, "$.next.createdAt"))
                        .param("afterId", JsonPath.<String>read(page1, "$.next.id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(contains(first.toString())))
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    @DisplayName("Пустая лента: 200, пустой список и next = null")
    void whenNoSubscriptionsThenEmptyFeed() throws Exception {
        mockMvc.perform(get("/api/v1/feed").param("followerId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isEmpty())
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    @DisplayName("Без followerId: 400 INVALID_POST_REQUEST")
    void whenFollowerIdMissingThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed")), 400, "INVALID_POST_REQUEST");
    }

    @ParameterizedTest(name = "limit={0}")
    @ValueSource(strings = {"0", "101", "abc"})
    @DisplayName("limit вне 1..100 или не число: 400 INVALID_POST_REQUEST")
    void whenLimitInvalidThenBadRequest(String limit) throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed")
                        .param("followerId", UUID.randomUUID().toString())
                        .param("limit", limit)),
                400, "INVALID_POST_REQUEST");
    }

    @Test
    @DisplayName("Курсор задан наполовину (только afterId): 400 INVALID_POST_REQUEST")
    void whenCursorPartialThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed")
                        .param("followerId", UUID.randomUUID().toString())
                        .param("afterId", UUID.randomUUID().toString())),
                400, "INVALID_POST_REQUEST");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "followerId=42",
            "followerId=00000000-0000-0000-0000-000000000001&afterCreatedAt=2026-10-15T10:00:00Z",
            "followerId=00000000-0000-0000-0000-000000000001"
                    + "&afterCreatedAt=yesterday&afterId=00000000-0000-0000-0000-000000000002",
            "followerId=00000000-0000-0000-0000-000000000001&afterCreatedAt=2026-10-15T10:00:00Z&afterId=42"
    })
    @DisplayName("followerId не UUID, курсор только из afterCreatedAt, afterCreatedAt не дата-время, "
            + "afterId не UUID: 400 INVALID_POST_REQUEST")
    void whenFeedParamInvalidThenBadRequest(String query) throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed?" + query)), 400, "INVALID_POST_REQUEST");
    }

    @Test
    @DisplayName("Границы limit: 1 и 100 допустимы")
    void whenLimitOnBoundsThenOk() throws Exception {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        post(author, 0);
        post(author, 1);
        subscriptions.subscribeIfAbsent(reader, author, BASE_TIME);

        mockMvc.perform(get("/api/v1/feed").param("followerId", reader.toString()).param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.next").isNotEmpty());
        mockMvc.perform(get("/api/v1/feed").param("followerId", reader.toString()).param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2))
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    @DisplayName("Без limit в ленте не больше 20 постов (значение по умолчанию из контракта)")
    void whenNoLimitThenDefaultLimit() throws Exception {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        for (int i = 0; i < 21; i++) {
            post(author, i);
        }
        subscriptions.subscribeIfAbsent(reader, author, BASE_TIME);

        mockMvc.perform(get("/api/v1/feed").param("followerId", reader.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(20))
                .andExpect(jsonPath("$.next").isNotEmpty());
    }

    private UUID post(UUID author, int minutes) {
        var time = BASE_TIME.plusSeconds(60L * minutes);
        return postRepository.createPost(new CreatePostRequest(
                UUID.randomUUID(), author, "Пост " + minutes, "Текст " + minutes,
                PostStatus.PUBLISHED, time, time
        )).id();
    }

    private static void expectError(ResultActions actions, int status, String code) throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
