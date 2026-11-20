package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendCursorException;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendsCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;

/**
 * Чтение страницы друзей пользователя: активные дружбы, где пользователь — любая сторона,
 * сначала новые ({@code created_at DESC, id DESC} дружбы). Страницы читаются по курсору,
 * как лента: без {@code OFFSET}, каждая страница стоит одинаково на любой глубине.
 * Список публичный: читатель не проверяется.
 */
@Service
@RequiredArgsConstructor
public class GetFriends {

    /** Максимальный размер страницы. */
    public static final int MAX_LIMIT = FriendshipsRepository.MAX_LIMIT;

    /** Хранилище дружб. */
    private final FriendshipsRepository friendshipsRepository;

    /** Преобразует страницу порта в результат. */
    private final FriendshipApplicationMapper mapper;

    /**
     * @param command пользователь, курсор предыдущей страницы (для первой — нет) и размер страницы
     * @return страница; её {@code next} — {@code null}, если друзей больше нет
     * @throws IncompleteFriendCursorException задана только одна из частей {@code afterCreatedAt}, {@code afterId}
     * @throws IllegalArgumentException        {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     */
    @Transactional(readOnly = true)
    public FriendPageResult get(GetFriendsCommand command) {
        if ((command.afterCreatedAt() == null) != (command.afterId() == null)) {
            throw new IncompleteFriendCursorException();
        }
        if (command.afterId() == null) {
            return mapper.toResult(friendshipsRepository.findFriendsPage(command.userId(), command.limit()));
        }
        FriendCursor after = new FriendCursor(command.afterCreatedAt(), command.afterId());
        return mapper.toResult(friendshipsRepository.findFriendsPageAfter(command.userId(), after, command.limit()));
    }
}
