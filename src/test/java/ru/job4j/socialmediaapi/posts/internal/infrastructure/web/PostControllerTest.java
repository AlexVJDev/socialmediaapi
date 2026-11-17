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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.shared.security.HeaderCurrentUserProvider;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Posts and subscriptions are prepared through the posts port and the subscriptions module API;
 * each test uses new user ids, since the container is shared between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("API постов и ленты: создание, изменение, удаление, чтение постов и ленты")
class PostControllerTest {

    /** Base time of posts; later posts are created minutes after it. */
    private static final Instant BASE_TIME = Instant.parse("2026-10-15T10:00:00Z");

    /** Заголовок текущего пользователя. */
    private static final String USER = HeaderCurrentUserProvider.HEADER;

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
    @DisplayName("Id поста не UUID: 400 VALIDATION_ERROR")
    void whenPostIdNotUuidThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get("/api/v1/posts/42")), 400, "VALIDATION_ERROR");
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

        mockMvc.perform(get("/api/v1/feed").header(USER, reader.toString()))
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
                        .header(USER, reader.toString())
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(contains(third.toString(), second.toString())))
                .andExpect(jsonPath("$.next.id").value(second.toString()))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/feed")
                        .header(USER, reader.toString())
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
        mockMvc.perform(get("/api/v1/feed").header(USER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isEmpty())
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @ParameterizedTest(name = "X-User-Id={0}")
    @ValueSource(strings = {"", "42"})
    @DisplayName("Лента без X-User-Id или с не-UUID: 401 UNAUTHORIZED")
    void whenFeedUserInvalidThenUnauthorized(String user) throws Exception {
        var request = get("/api/v1/feed");
        if (!user.isEmpty()) {
            request.header(USER, user);
        }
        expectError(mockMvc.perform(request), 401, "UNAUTHORIZED");
    }

    @ParameterizedTest(name = "limit={0}")
    @ValueSource(strings = {"0", "101", "abc"})
    @DisplayName("limit вне 1..100 или не число: 400 VALIDATION_ERROR")
    void whenLimitInvalidThenBadRequest(String limit) throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed")
                        .header(USER, UUID.randomUUID().toString())
                        .param("limit", limit)),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Курсор задан наполовину (только afterId): 400 VALIDATION_ERROR")
    void whenCursorPartialThenBadRequest() throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed")
                        .header(USER, UUID.randomUUID().toString())
                        .param("afterId", UUID.randomUUID().toString())),
                400, "VALIDATION_ERROR");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "afterCreatedAt=2026-10-15T10:00:00Z",
            "afterCreatedAt=yesterday&afterId=00000000-0000-0000-0000-000000000002",
            "afterCreatedAt=2026-10-15T10:00:00Z&afterId=42"
    })
    @DisplayName("Курсор только из afterCreatedAt, afterCreatedAt не дата-время, "
            + "afterId не UUID: 400 VALIDATION_ERROR")
    void whenFeedParamInvalidThenBadRequest(String query) throws Exception {
        expectError(mockMvc.perform(get("/api/v1/feed?" + query).header(USER, UUID.randomUUID().toString())),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Границы limit: 1 и 100 допустимы")
    void whenLimitOnBoundsThenOk() throws Exception {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        post(author, 0);
        post(author, 1);
        subscriptions.subscribeIfAbsent(reader, author, BASE_TIME);

        mockMvc.perform(get("/api/v1/feed").header(USER, reader.toString()).param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.next").isNotEmpty());
        mockMvc.perform(get("/api/v1/feed").header(USER, reader.toString()).param("limit", "100"))
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

        mockMvc.perform(get("/api/v1/feed").header(USER, reader.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(20))
                .andExpect(jsonPath("$.next").isNotEmpty());
    }

    @Test
    @DisplayName("POST /posts: 201, Location и пост текущего пользователя со статусом PUBLISHED")
    void whenCreatePostThenCreated() throws Exception {
        var author = UUID.randomUUID();

        var body = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts").header(USER, author.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Заголовок", "Текст")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(author.toString()))
                .andExpect(jsonPath("$.title").value("Заголовок"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn().getResponse();

        var id = JsonPath.<String>read(body.getContentAsString(), "$.id");
        assertThat(body.getHeader("Location")).isEqualTo("/api/v1/posts/" + id);
        mockMvc.perform(get("/api/v1/posts/{id}", id)).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"content\":\"Текст\"}",
            "{\"title\":\"   \",\"content\":\"Текст\"}",
            "{\"title\":\"\",\"content\":\"Текст\"}",
            "not json"
    })
    @DisplayName("POST /posts с некорректным телом: 400 VALIDATION_ERROR")
    void whenCreatePostInvalidBodyThenBadRequest(String json) throws Exception {
        expectError(mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header(USER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(json)),
                400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("POST /posts без X-User-Id: 401 UNAUTHORIZED")
    void whenCreatePostWithoutUserThenUnauthorized() throws Exception {
        expectError(mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON).content(postJson("Заголовок", "Текст"))),
                401, "UNAUTHORIZED");
    }

    @Test
    @DisplayName("PUT /posts/{id} автором: 200 и новые заголовок и текст")
    void whenAuthorUpdatesPostThenOk() throws Exception {
        var author = UUID.randomUUID();
        var postId = post(author, 0);

        mockMvc.perform(put("/api/v1/posts/{id}", postId).header(USER, author.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Новый", "Новый текст")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()))
                .andExpect(jsonPath("$.title").value("Новый"))
                .andExpect(jsonPath("$.content").value("Новый текст"));
    }

    @Test
    @DisplayName("PUT и DELETE чужого поста: 404 POST_NOT_FOUND, пост не изменился")
    void whenOtherUserChangesPostThenNotFound() throws Exception {
        var postId = post(UUID.randomUUID(), 0);
        var other = UUID.randomUUID().toString();

        expectError(mockMvc.perform(put("/api/v1/posts/{id}", postId).header(USER, other)
                        .contentType(MediaType.APPLICATION_JSON).content(postJson("Чужой", "Чужой"))),
                404, "POST_NOT_FOUND");
        expectError(mockMvc.perform(delete("/api/v1/posts/{id}", postId).header(USER, other)),
                404, "POST_NOT_FOUND");
        mockMvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Пост 0"));
    }

    @Test
    @DisplayName("DELETE /posts/{id} автором: 204, пост больше не читается, повторное удаление — 404")
    void whenAuthorDeletesPostThenNoContent() throws Exception {
        var author = UUID.randomUUID();
        var postId = post(author, 0);

        mockMvc.perform(delete("/api/v1/posts/{id}", postId).header(USER, author.toString()))
                .andExpect(status().isNoContent());
        expectError(mockMvc.perform(get("/api/v1/posts/{id}", postId)), 404, "POST_NOT_FOUND");
        expectError(mockMvc.perform(delete("/api/v1/posts/{id}", postId).header(USER, author.toString())),
                404, "POST_NOT_FOUND");
    }

    @Test
    @DisplayName("GET /users/{id}/posts: посты пользователя по страницам, без заголовка X-User-Id")
    void whenUserPostsThenPagedNewestFirst() throws Exception {
        var author = UUID.randomUUID();
        var first = post(author, 0);
        var second = post(author, 1);
        var third = post(author, 2);
        post(UUID.randomUUID(), 3);

        var page1 = mockMvc.perform(get("/api/v1/users/{id}/posts", author).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(contains(third.toString(), second.toString())))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/users/{id}/posts", author)
                        .param("limit", "2")
                        .param("afterCreatedAt", JsonPath.<String>read(page1, "$.next.createdAt"))
                        .param("afterId", JsonPath.<String>read(page1, "$.next.id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].id").value(contains(first.toString())))
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    @DisplayName("GET /users/{id}/posts неизвестного пользователя: 200 и пустая страница")
    void whenUnknownUserPostsThenEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}/posts", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isEmpty())
                .andExpect(jsonPath("$.next").value(nullValue()));
    }

    private static String postJson(String title, String content) {
        return "{\"title\":\"%s\",\"content\":\"%s\"}".formatted(title, content);
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
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
