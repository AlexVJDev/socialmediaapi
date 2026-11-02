package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;

import java.time.Instant;
import java.util.ArrayList;
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
@DisplayName("Лента: опубликованные посты авторов из подписок читателя, от новых к старым, страницы по курсору")
class FeedPersistenceAdapterTest {

    /** Base time of posts; later posts are created minutes after it. */
    private static final Instant BASE_TIME = Instant.parse("2026-10-15T10:00:00Z");

    /** Page size that fits all posts of a test. */
    private static final int ALL = FeedRepository.MAX_LIMIT;

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

        assertThat(feedIds(reader)).isEmpty();
    }

    @Test
    @DisplayName("После подписки пост автора появляется в ленте читателя")
    void whenSubscribedThenPostInFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var post = post(author, 0);

        subscribe(reader, author);

        assertThat(feedIds(reader)).containsExactly(post);
    }

    @Test
    @DisplayName("После отписки пост автора исчезает из ленты, сама публикация остаётся")
    void whenUnsubscribedThenPostDisappearsFromFeed() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var post = post(author, 0);
        subscribe(reader, author);

        subscriptions.unsubscribe(reader, author);

        assertThat(feedIds(reader)).isEmpty();
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

        delete(deleted, author);

        assertThat(feedIds(reader)).containsExactly(kept);
    }

    @Test
    // SQL of the feed for two mutual subscriptions; the friendship process itself (accept creates them)
    // is checked end to end by FriendshipFeedTest in the root test package.
    @DisplayName("Взаимные подписки (их создаёт принятие дружбы): каждый видит посты другого, но не свои")
    void whenMutualSubscriptionsThenEachSeesPostsOfTheOther() {
        var alice = UUID.randomUUID();
        var bob = UUID.randomUUID();
        var alicePost = post(alice, 0);
        var bobPost = post(bob, 1);

        subscribe(alice, bob);
        subscribe(bob, alice);

        assertThat(feedIds(alice)).containsExactly(bobPost);
        assertThat(feedIds(bob)).containsExactly(alicePost);
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

        assertThat(feedIds(reader)).containsExactly(followed);
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

        var page = feedRepository.findFeed(reader, ALL);

        assertThat(ids(page)).containsExactly(newest, middle, oldest);
        assertThat(page.posts().getFirst()).isEqualTo(new FeedPostResponse(
                newest, first, "Заголовок", "Текст",
                PostStatus.PUBLISHED, BASE_TIME.plusSeconds(120), BASE_TIME.plusSeconds(120)
        ));
    }

    @Test
    @DisplayName("Посты с одинаковым created_at идут по id DESC")
    void whenSameCreatedAtThenOrderedByIdDesc() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var lower = idWithFirstDigit(1);
        var higher = idWithFirstDigit(2);
        post(lower, author, BASE_TIME);
        post(higher, author, BASE_TIME);
        subscribe(reader, author);

        assertThat(feedIds(reader)).containsExactly(higher, lower);
    }

    @Test
    @DisplayName("Страницы по курсору: 5 постов по 2 дают 2 + 2 + 1 без повторов и пропусков, у последней next пуст")
    void whenPagedByCursorThenPagesFollowTheOrderWithoutGaps() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var posts = List.of(post(author, 4), post(author, 3), post(author, 2), post(author, 1), post(author, 0));
        subscribe(reader, author);

        var first = feedRepository.findFeed(reader, 2);
        var second = feedRepository.findFeedAfter(reader, first.next().orElseThrow(), 2);
        var third = feedRepository.findFeedAfter(reader, second.next().orElseThrow(), 2);

        assertThat(ids(first)).containsExactly(posts.get(0), posts.get(1));
        assertThat(ids(second)).containsExactly(posts.get(2), posts.get(3));
        assertThat(ids(third)).containsExactly(posts.get(4));
        assertThat(third.next()).isEmpty();
    }

    @Test
    @DisplayName("next — позиция последнего поста страницы: его created_at и id")
    void whenMorePostsThanLimitThenNextPointsAtLastPostOfPage() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        post(author, 2);
        var last = post(author, 1);
        post(author, 0);
        subscribe(reader, author);

        var page = feedRepository.findFeed(reader, 2);

        assertThat(page.next()).contains(new FeedCursor(BASE_TIME.plusSeconds(60), last));
    }

    @Test
    @DisplayName("Ровно limit постов: одна страница, next пуст (лишняя строка limit + 1 не пришла)")
    void whenExactlyLimitPostsThenNoNextPage() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var newer = post(author, 1);
        var older = post(author, 0);
        subscribe(reader, author);

        var page = feedRepository.findFeed(reader, 2);

        assertThat(ids(page)).containsExactly(newer, older);
        assertThat(page.next()).isEmpty();
    }

    @Test
    @DisplayName("Пустая лента (нет подписок): нет постов, next пуст")
    void whenNoSubscriptionsThenEmptyPage() {
        var page = feedRepository.findFeed(UUID.randomUUID(), ALL);

        assertThat(page.posts()).isEmpty();
        assertThat(page.next()).isEmpty();
    }

    @Test
    @DisplayName("Одинаковый created_at у постов разных авторов на границах страниц: каждый пост ровно один раз")
    void whenSameCreatedAtAcrossAuthorsThenWalkHasNoDuplicatesOrGaps() {
        var reader = UUID.randomUUID();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var p1 = idWithFirstDigit(1);
        var p2 = idWithFirstDigit(2);
        var p3 = idWithFirstDigit(3);
        var p4 = idWithFirstDigit(4);
        post(p1, first, BASE_TIME);
        post(p2, second, BASE_TIME);
        post(p3, first, BASE_TIME);
        post(p4, second, BASE_TIME);
        subscribe(reader, first);
        subscribe(reader, second);

        assertThat(walk(reader, 1)).containsExactly(List.of(p4), List.of(p3), List.of(p2), List.of(p1));
        assertThat(walk(reader, 3)).containsExactly(List.of(p4, p3, p2), List.of(p1));
    }

    @Test
    @DisplayName("У одного автора постов больше limit, у другого меньше: страницы в общем порядке ленты")
    void whenAuthorsHaveDifferentNumberOfPostsThenPagesFollowGlobalOrder() {
        var reader = UUID.randomUUID();
        var prolific = UUID.randomUUID();
        var rare = UUID.randomUUID();
        var a0 = post(prolific, 0);
        var a2 = post(prolific, 2);
        var b3 = post(rare, 3);
        var a4 = post(prolific, 4);
        var a6 = post(prolific, 6);
        var b7 = post(rare, 7);
        var a8 = post(prolific, 8);
        subscribe(reader, prolific);
        subscribe(reader, rare);

        assertThat(walk(reader, 2)).containsExactly(
                List.of(a8, b7), List.of(a6, a4), List.of(b3, a2), List.of(a0)
        );
    }

    @Test
    @DisplayName("Пост, опубликованный после первой страницы, не попадает на следующие; виден в новой первой")
    void whenPostPublishedAfterFirstPageThenNotOnNextPage() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var p2 = post(author, 2);
        var p1 = post(author, 1);
        var p0 = post(author, 0);
        subscribe(reader, author);
        var first = feedRepository.findFeed(reader, 2);

        var fresh = post(author, 10);
        var second = feedRepository.findFeedAfter(reader, first.next().orElseThrow(), 2);

        assertThat(ids(first)).containsExactly(p2, p1);
        assertThat(ids(second)).containsExactly(p0);
        assertThat(second.next()).isEmpty();
        assertThat(feedIds(reader)).containsExactly(fresh, p2, p1, p0);
    }

    @Test
    @DisplayName("Пост следующей страницы удалён между запросами: страница без пропуска")
    void whenPostOfNextPageDeletedThenNextPageHasNoGap() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var posts = List.of(post(author, 4), post(author, 3), post(author, 2), post(author, 1), post(author, 0));
        subscribe(reader, author);
        var first = feedRepository.findFeed(reader, 2);

        delete(posts.get(2), author);
        var second = feedRepository.findFeedAfter(reader, first.next().orElseThrow(), 2);

        assertThat(ids(second)).containsExactly(posts.get(3), posts.get(4));
    }

    @Test
    @DisplayName("Удалён сам пост курсора: следующая страница читается от его позиции, как обычно")
    void whenCursorPostDeletedThenNextPageStillFollowsPosition() {
        var reader = UUID.randomUUID();
        var author = UUID.randomUUID();
        var posts = List.of(post(author, 3), post(author, 2), post(author, 1), post(author, 0));
        subscribe(reader, author);
        var first = feedRepository.findFeed(reader, 2);

        delete(posts.get(1), author);
        var second = feedRepository.findFeedAfter(reader, first.next().orElseThrow(), 2);

        assertThat(ids(second)).containsExactly(posts.get(2), posts.get(3));
    }

    @Test
    @DisplayName("Отписка между страницами: посты этого автора дальше не приходят, посты остальных — без пропусков")
    void whenUnsubscribedBetweenPagesThenNextPagesSkipOnlyThatAuthor() {
        var reader = UUID.randomUUID();
        var kept = UUID.randomUUID();
        var dropped = UUID.randomUUID();
        var k5 = post(kept, 5);
        var d4 = post(dropped, 4);
        var k3 = post(kept, 3);
        post(dropped, 2);
        var k1 = post(kept, 1);
        post(dropped, 0);
        subscribe(reader, kept);
        subscribe(reader, dropped);
        var first = feedRepository.findFeed(reader, 2);

        subscriptions.unsubscribe(reader, dropped);
        var second = feedRepository.findFeedAfter(reader, first.next().orElseThrow(), 2);

        assertThat(ids(first)).containsExactly(k5, d4);
        assertThat(ids(second)).containsExactly(k3, k1);
        assertThat(second.next()).isEmpty();
    }

    @Test
    @DisplayName("limit < 1 отклоняется IllegalArgumentException")
    void whenLimitNotPositiveThenThrowException() {
        var reader = UUID.randomUUID();
        var cursor = new FeedCursor(BASE_TIME, UUID.randomUUID());

        assertThatThrownBy(() -> feedRepository.findFeed(reader, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> feedRepository.findFeed(reader, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> feedRepository.findFeedAfter(reader, cursor, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("limit > MAX_LIMIT отклоняется IllegalArgumentException, limit = MAX_LIMIT допустим")
    void whenLimitAboveMaximumThenThrowException() {
        var reader = UUID.randomUUID();
        var cursor = new FeedCursor(BASE_TIME, UUID.randomUUID());

        assertThatThrownBy(() -> feedRepository.findFeed(reader, FeedRepository.MAX_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> feedRepository.findFeedAfter(reader, cursor, FeedRepository.MAX_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(feedRepository.findFeed(reader, FeedRepository.MAX_LIMIT).posts()).isEmpty();
    }

    @Test
    @DisplayName("findFeedAfter без курсора (null) отклоняется NullPointerException")
    void whenCursorNullThenThrowException() {
        assertThatThrownBy(() -> feedRepository.findFeedAfter(UUID.randomUUID(), null, 2))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("after");
    }

    /**
     * Ids of the whole feed of the reader in one page.
     *
     * @param reader reader of the feed
     * @return ids of the posts, newest first
     */
    private List<UUID> feedIds(UUID reader) {
        var page = feedRepository.findFeed(reader, ALL);
        assertThat(page.next()).isEmpty();
        return ids(page);
    }

    /**
     * Reads the whole feed page by page through the cursor.
     *
     * @param reader reader of the feed
     * @param limit  page size
     * @return ids of the posts of every page, in the order of the pages
     */
    private List<List<UUID>> walk(UUID reader, int limit) {
        var pages = new ArrayList<List<UUID>>();
        var page = feedRepository.findFeed(reader, limit);
        pages.add(ids(page));
        while (page.next().isPresent()) {
            page = feedRepository.findFeedAfter(reader, page.next().get(), limit);
            pages.add(ids(page));
        }
        return pages;
    }

    private static List<UUID> ids(FeedPageResponse page) {
        return page.posts().stream().map(FeedPostResponse::id).toList();
    }

    private void subscribe(UUID follower, UUID followed) {
        assertThat(subscriptions.subscribeIfAbsent(follower, followed, BASE_TIME)).isTrue();
    }

    private void delete(UUID post, UUID author) {
        assertThat(postRepository.deletePost(new DeletePostRequest(post, author, BASE_TIME.plusSeconds(3600))))
                .isTrue();
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
     * Id whose order is set by its first hex digit: PostgreSQL compares UUIDs as unsigned bytes,
     * so ids that differ in the first digit are ordered by it. The random tail keeps the ids
     * unique between runs, as the container is shared.
     *
     * @param digit first hex digit, 1–9
     * @return the id
     */
    private static UUID idWithFirstDigit(int digit) {
        return UUID.fromString(digit + "0000000-0000-0000-0000-" + UUID.randomUUID().toString().substring(24));
    }
}
