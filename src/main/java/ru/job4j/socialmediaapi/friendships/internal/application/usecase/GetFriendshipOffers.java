package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendshipOfferCursorException;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOffersCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;

/**
 * Чтение страницы заявок в друзья текущего пользователя: входящих (пользователь получатель)
 * или исходящих (пользователь отправитель) в одном статусе, сначала новые ({@code created_at DESC, id DESC}).
 * Страницы читаются по курсору, как лента: без {@code OFFSET}, каждая страница стоит одинаково на любой глубине.
 * Заявки других пользователей не читаются.
 */
@Service
@RequiredArgsConstructor
public class GetFriendshipOffers {

    /** Размер страницы, если клиент его не задал. */
    public static final int DEFAULT_LIMIT = 20;

    /** Максимальный размер страницы. */
    public static final int MAX_LIMIT = FriendshipOfferRepository.MAX_LIMIT;

    /** Хранилище заявок в друзья. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Преобразует страницу порта в результат. */
    private final FriendshipOfferApplicationMapper mapper;

    /**
     * @param command пользователь, направление, статус, курсор предыдущей страницы (для первой — нет)
     *                и размер страницы
     * @return страница; её {@code next} — {@code null}, если заявок больше нет
     * @throws IncompleteFriendshipOfferCursorException задана только одна часть курсора
     *                                                  ({@code afterCreatedAt} или {@code afterId})
     * @throws IllegalArgumentException                 {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     */
    @Transactional(readOnly = true)
    public FriendshipOfferPageResult get(GetFriendshipOffersCommand command) {
        if ((command.afterCreatedAt() == null) != (command.afterId() == null)) {
            throw new IncompleteFriendshipOfferCursorException();
        }
        if (command.afterId() == null) {
            return mapper.toResult(friendshipOfferRepository.findPage(
                    command.userId(), command.direction(), command.status(), command.limit()));
        }
        FriendshipOfferCursor after = new FriendshipOfferCursor(command.afterCreatedAt(), command.afterId());
        return mapper.toResult(friendshipOfferRepository.findPageAfter(
                command.userId(), command.direction(), command.status(), after, command.limit()));
    }
}
