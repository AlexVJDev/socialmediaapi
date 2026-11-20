package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendPageResponse;

import java.util.UUID;

public interface FriendshipsRepository {

    /** Максимальный размер страницы {@link #findFriendsPage} и {@link #findFriendsPageAfter}. */
    int MAX_LIMIT = 100;

    /**
     * Сохраняет дружбу двух пользователей в каноническом порядке.
     * У пары пользователей не больше одной заявки, поэтому её принятие не может встретить
     * существующую дружбу пары: дубликат здесь — ошибка данных, вызывающий его не перехватывает,
     * и транзакция откатывается.
     *
     * @param request дружба для сохранения
     * @return сохранённая дружба
     * @throws FriendshipAlreadyExistsException дружба этих пользователей уже есть
     */
    CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    );

    /**
     * Первая страница активных дружб {@code userId}, сначала новые ({@code created_at DESC, id DESC}).
     * Пользователь может быть любой стороной дружбы; дружбы других пользователей не читаются.
     *
     * @param userId пользователь, чьи друзья читаются
     * @param limit  максимальное число друзей на странице, от 1 до {@link #MAX_LIMIT}
     * @return страница; её {@code next} пуст, если друзей больше нет
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     * @throws NullPointerException     {@code userId} — {@code null}
     */
    FriendPageResponse findFriendsPage(UUID userId, int limit);

    /**
     * Страница тех же дружб, что и {@link #findFriendsPage}, строго после {@code after} в том же порядке.
     *
     * @param userId пользователь, чьи друзья читаются
     * @param after  позиция последней показанной дружбы из {@link FriendPageResponse#next()}
     * @param limit  максимальное число друзей на странице, от 1 до {@link #MAX_LIMIT}
     * @return страница; её {@code next} пуст, если друзей больше нет
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     * @throws NullPointerException     {@code userId} или {@code after} — {@code null}
     */
    FriendPageResponse findFriendsPageAfter(UUID userId, FriendCursor after, int limit);
}
