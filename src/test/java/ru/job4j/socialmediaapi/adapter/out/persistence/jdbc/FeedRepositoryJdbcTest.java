package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.application.port.out.FeedRepository;
import ru.job4j.socialmediaapi.application.port.out.FeedRepository.FeedPostResponse;
import ru.job4j.socialmediaapi.application.port.out.PostRepository;
import ru.job4j.socialmediaapi.application.port.out.PostRepository.CreatePostRequest;
import ru.job4j.socialmediaapi.application.port.out.PostRepository.DeletePostRequest;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;
import ru.job4j.socialmediaapi.domain.post.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Data is prepared through the posts port and the subscriptions module API, so the tests check
 * how the feed follows the business process, not only the SQL. Each test uses new
 * user ids: the container is shared and holds the data of other tests.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("Лента: опубликованные посты авторов из подписок читателя, от новых к старым")
class FeedRepositoryJdbcTest {

    /** Base time of posts; later posts are created minutes after it. */
    private static final Instant BASE_TIME = Instant.parse("2026-10-15T10:00:00Z");

    /** Page size that fits all posts of a test. */
    private static final int ALL = 100;

    /** Repository under test. */
    @Autowired
    private FeedRepository feedRepository;

    /** Creates and deletes posts. */
    @Autowired
    private PostRepository postRepository;

    /** Subscribes and unsubscribes readers (API of the subscriptions module). */
    @Autowired
    private Subscriptions subscriptions;

    @Test
    @DisplayName("До подписки пост автора отсутствует в ленте читателя")
    void whenNotSubscribedThenPostNotInFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        post(author, 0);

        assertThat(feed(reader)).isEmpty();
    }

    @Test
    @DisplayName("После подписки пост автора появляется в ленте читателя")
    void whenSubscribedThenPostInFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var post = post(author, 0);

        subscribe(reader, author);

        assertThat(ids(feed(reader))).containsExactly(post);
    }

    @Test
    @DisplayName("После отписки пост автора исчезает из ленты, сама публикация остаётся")
    void whenUnsubscribedThenPostDisappearsFromFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var post = post(author, 0);
        subscribe(reader, author);

        subscriptions.unsubscribe(reader, author);

        assertThat(feed(reader)).isEmpty();
        assertThat(postRepository.findPublishedById(post)).isPresent();
    }

    @Test
    @DisplayName("Удалённый автором пост (DELETED) исключается из ленты, остальные посты автора остаются")
    void whenPostDeletedThenExcludedFromFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var kept = post(author, 0);
        var deleted = post(author, 1);
        subscribe(reader, author);

        postRepository.deletePost(new DeletePostRequest(deleted, author, BASE_TIME.plusSeconds(3600)));

        assertThat(ids(feed(reader))).containsExactly(kept);
    }

    @Test
    @DisplayName("Взаимные подписки (как после принятия дружбы): каждый видит посты другого, но не свои")
    void whenMutualSubscriptionsThenEachSeesPostsOfTheOther() {
        var alice = UUID.randomUUID();
        var bob = UUID.randomUUID();
        var alicePost = post(alice, 0);
        var bobPost = post(bob, 1);

        subscribe(alice, bob);
        subscribe(bob, alice);

        assertThat(ids(feed(alice))).containsExactly(bobPost);
        assertThat(ids(feed(bob))).containsExactly(alicePost);
    }

    @Test
    @DisplayName("В ленте только посты авторов из подписок: посты постороннего автора и свои посты не попадают")
    void whenOtherAuthorsAndOwnPostsThenNotInFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var followed = post(author, 0);
        post(UUID.randomUUID(), 1);
        post(reader, 2);
        subscribe(reader, author);

        assertThat(ids(feed(reader))).containsExactly(followed);
    }

    @Test
    @DisplayName("Посты разных авторов идут от новых к старым по created_at, все поля переносятся")
    void whenSeveralAuthorsThenNewestFirst() {
        var reader = UUID.randomUUID();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var oldest = post(first, 0);
        var middle = post(second, 1);
        var newest = post(first, 2);
        subscribe(reader, first);
        subscribe(reader, second);

        var feed = feed(reader);

        assertThat(ids(feed)).containsExactly(newest, middle, oldest);
        assertThat(feed.getFirst()).isEqualTo(new FeedPostResponse(
                newest, first, "Заголовок", "Текст",
                PostStatus.PUBLISHED, BASE_TIME.plusSeconds(120), BASE_TIME.plusSeconds(120)
        ));
    }

    @Test
    @DisplayName("Посты с одинаковым created_at идут по id DESC")
    void whenSameCreatedAtThenOrderedByIdDesc() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        // The ids differ in the first hex digit, so PostgreSQL (unsigned bytes) and the string form agree
        var lower = UUID.fromString("10000000-0000-0000-0000-" + suffix());
        var higher = UUID.fromString("20000000-0000-0000-0000-" + suffix());
        post(lower, author, BASE_TIME);
        post(higher, author, BASE_TIME);
        subscribe(reader, author);

        assertThat(ids(feed(reader))).containsExactly(higher, lower);
    }

    @Test
    @DisplayName("Страницы limit/offset: 5 постов по 2 дают 2 + 2 + 1 без повторов и пропусков, дальше — пусто")
    void whenPagedThenPagesFollowTheOrderWithoutGaps() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var posts = List.of(post(author, 4), post(author, 3), post(author, 2), post(author, 1), post(author, 0));
        subscribe(reader, author);

        var first = feedRepository.findFeed(reader, 2, 0);
        var second = feedRepository.findFeed(reader, 2, 2);
        var third = feedRepository.findFeed(reader, 2, 4);
        var beyond = feedRepository.findFeed(reader, 2, 6);

        assertThat(ids(first)).containsExactly(posts.get(0), posts.get(1));
        assertThat(ids(second)).containsExactly(posts.get(2), posts.get(3));
        assertThat(ids(third)).containsExactly(posts.get(4));
        assertThat(beyond).isEmpty();
    }

    @Test
    @DisplayName("limit <= 0 отклоняется IllegalArgumentException")
    void whenLimitNotPositiveThenThrowException() {
        var reader = UUID.randomUUID();

        assertThatThrownBy(() -> feedRepository.findFeed(reader, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> feedRepository.findFeed(reader, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("offset < 0 отклоняется IllegalArgumentException")
    void whenOffsetNegativeThenThrowException() {
        assertThatThrownBy(() -> feedRepository.findFeed(UUID.randomUUID(), 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private List<FeedPostResponse> feed(UUID reader) {
        return feedRepository.findFeed(reader, ALL, 0);
    }

    private static List<UUID> ids(List<FeedPostResponse> feed) {
        return feed.stream().map(FeedPostResponse::id).toList();
    }

    private void subscribe(UUID follower, UUID followed) {
        assertThat(subscriptions.subscribeIfAbsent(follower, followed, BASE_TIME)).isTrue();
    }

    /**
     * Publishes a post of the author.
     *
     * @param author  author of the post
     * @param minutes minutes after {@link #BASE_TIME} the post is created
     * @return id of the post
     */
    private UUID post(UUID author, int minutes) {
        return post(UUID.randomUUID(), author, BASE_TIME.plusSeconds(minutes * 60L));
    }

    private UUID post(UUID id, UUID author, Instant createdAt) {
        return postRepository.createPost(new CreatePostRequest(
                id, author, "Заголовок", "Текст", PostStatus.PUBLISHED, createdAt, createdAt
        )).id();
    }

    /**
     * @return random last 12 hex digits of a UUID, so fixed-prefix ids do not clash between runs
     */
    private static String suffix() {
        return UUID.randomUUID().toString().substring(24);
    }
}
