package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.CreatePost;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.DeletePost;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetPost;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetUserPosts;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.UpdatePost;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper.PostWebMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.request.PostWebRequest;
import ru.job4j.socialmediaapi.shared.security.CurrentUserProvider;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Эндпоинты постов: {@code POST /api/v1/posts}, {@code GET/PUT/DELETE /api/v1/posts/{postId}},
 * {@code GET /api/v1/users/{userId}/posts}.
 * Пути, методы, параметры и их проверка берутся из {@link PostApi}, сгенерированного из контракта
 * ({@code docs/api/paths/post-create.yaml}, {@code post-by-id.yaml}, {@code posts-by-user.yaml});
 * аннотации здесь не повторяются.
 * <p>
 * Автор при создании, изменении и удалении — текущий пользователь из {@link CurrentUserProvider};
 * из тела или параметров запроса он не берётся. Чтение публичное: пользователь не нужен.
 * Ошибки ({@code 400}, {@code 404}) отвечает {@code PostExceptionHandler}, {@code 401} — общий обработчик.
 */
@RestController
@RequiredArgsConstructor
public class PostController implements PostApi {

    /** Путь поста для заголовка {@code Location}. */
    private static final String POST_PATH = "/api/v1/posts/";

    /** Use case that reads a post. */
    private final GetPost getPost;

    /** Публикует пост. */
    private final CreatePost createPost;

    /** Меняет пост. */
    private final UpdatePost updatePost;

    /** Удаляет пост. */
    private final DeletePost deletePost;

    /** Читает страницу постов пользователя. */
    private final GetUserPosts getUserPosts;

    /** Текущий пользователь. */
    private final CurrentUserProvider currentUser;

    /** Maps web DTOs to the scenario command and the result back. */
    private final PostWebMapper mapper;

    /**
     * {@code 200} with the published post. A missing or deleted post ({@code 404}) and an id
     * that is not a UUID ({@code 400}) are answered by {@code PostExceptionHandler}.
     *
     * @param postId id of the post
     * @return the post
     */
    @Override
    public ResponseEntity<?> getPost(UUID postId) {
        return ResponseEntity.ok(mapper.toResponse(getPost.get(mapper.toGetPostCommand(postId))));
    }

    /**
     * {@code 201} с опубликованным постом и заголовком {@code Location}.
     *
     * @param request заголовок и текст
     * @return опубликованный пост
     */
    @Override
    public ResponseEntity<?> createPost(PostWebRequest request) {
        PostResult created = createPost.create(
                mapper.toCreatePostCommand(currentUser.currentUserId(), request));
        return ResponseEntity.created(URI.create(POST_PATH + created.id()))
                .body(mapper.toResponse(created));
    }

    /**
     * {@code 200} с изменённым постом. Чужой, удалённый или отсутствующий пост — {@code 404}.
     *
     * @param postId  пост
     * @param request новые заголовок и текст
     * @return изменённый пост
     */
    @Override
    public ResponseEntity<?> updatePost(UUID postId, PostWebRequest request) {
        return ResponseEntity.ok(mapper.toResponse(updatePost.update(
                mapper.toUpdatePostCommand(postId, currentUser.currentUserId(), request))));
    }

    /**
     * {@code 204}. Чужой, уже удалённый или отсутствующий пост — {@code 404}.
     *
     * @param postId пост
     * @return пустой ответ
     */
    @Override
    public ResponseEntity<?> deletePost(UUID postId) {
        deletePost.delete(mapper.toDeletePostCommand(postId, currentUser.currentUserId()));
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code 200} со страницей опубликованных постов пользователя; у неизвестного пользователя — пустая.
     *
     * @param userId         автор постов
     * @param limit          размер страницы, от 1 до 100 (20, если не задан)
     * @param afterCreatedAt {@code next.createdAt} предыдущей страницы
     * @param afterId        {@code next.id} предыдущей страницы
     * @return страница постов
     */
    @Override
    public ResponseEntity<?> getUserPosts(UUID userId, Integer limit, Instant afterCreatedAt, UUID afterId) {
        return ResponseEntity.ok(mapper.toResponse(getUserPosts.get(
                mapper.toGetUserPostsCommand(userId, limit, afterCreatedAt, afterId))));
    }
}
