package ru.job4j.socialmediaapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end process of two modules: an accepted friendship ({@code friendships}) creates mutual
 * subscriptions, so each friend sees the posts of the other in the feed ({@code posts}).
 * Lives in the root package because it uses both modules; posts are created through
 * the {@code posts} port, as the module has no REST API yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("Дружба и лента: после принятия заявки оба друга видят посты друг друга")
class FriendshipFeedTest {

    /** Base URL of the friendship offer endpoints. */
    private static final String OFFERS = "/api/v1/friendship-offer";

    /** Fixed creation time of the posts. */
    private static final Instant POSTED_AT = Instant.parse("2026-10-16T10:15:00Z");

    /** MockMvc to call the API without a running server. */
    @Autowired
    private MockMvc mockMvc;

    /** Creates posts of the friends. */
    @Autowired
    private PostRepository postRepository;

    /** Reads the feeds of the friends. */
    @Autowired
    private FeedRepository feedRepository;

    @Test
    @DisplayName("До принятия пост Бориса только у Алисы (подписка с заявки); после accept — посты друг друга у обоих")
    void whenOfferAcceptedThenFriendsSeePostsOfEachOther() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        var alicePost = publish(aliceId, 0);
        var borisPost = publish(borisId, 1);
        var offerId = createOffer(aliceId, borisId);

        assertThat(feedIds(aliceId)).containsExactly(borisPost);
        assertThat(feedIds(borisId)).isEmpty();

        mockMvc.perform(post(OFFERS + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offerId\":\"%s\",\"actorId\":\"%s\"}".formatted(offerId, borisId)))
                .andExpect(status().isOk());

        assertThat(feedIds(aliceId)).containsExactly(borisPost);
        assertThat(feedIds(borisId)).containsExactly(alicePost);
    }

    @Test
    @DisplayName("После reject лента не меняется: отправитель видит посты получателя, получатель — нет")
    void whenOfferRejectedThenOnlySenderSeesPosts() throws Exception {
        var aliceId = UUID.randomUUID();
        var borisId = UUID.randomUUID();
        publish(aliceId, 0);
        var borisPost = publish(borisId, 1);
        var offerId = createOffer(aliceId, borisId);

        mockMvc.perform(post(OFFERS + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offerId\":\"%s\",\"actorId\":\"%s\"}".formatted(offerId, borisId)))
                .andExpect(status().isOk());

        assertThat(feedIds(aliceId)).containsExactly(borisPost);
        assertThat(feedIds(borisId)).isEmpty();
    }

    private UUID createOffer(UUID fromUserId, UUID toUserId) throws Exception {
        var json = mockMvc.perform(post(OFFERS + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromUserId\":\"%s\",\"toUserId\":\"%s\"}".formatted(fromUserId, toUserId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID publish(UUID authorId, int minutes) {
        var createdAt = POSTED_AT.plusSeconds(60L * minutes);
        return postRepository.createPost(new CreatePostRequest(
                UUID.randomUUID(), authorId, "title", "content", PostStatus.PUBLISHED, createdAt, createdAt
        )).id();
    }

    private List<UUID> feedIds(UUID readerId) {
        return feedRepository.findFeed(readerId, 10, 0).stream().map(FeedPostResponse::id).toList();
    }
}
