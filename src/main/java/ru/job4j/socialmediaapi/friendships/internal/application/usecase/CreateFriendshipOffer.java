package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;

import java.time.Clock;

/**
 * Создание заявки в друзья, см. «1. Создание заявки» в {@code docs/process/friendship_transitions.puml}.
 *
 * <p>Заявка вставляется сразу, без предварительного поиска: у пары пользователей не больше одной заявки
 * в любом направлении (уникальный индекс таблицы), поэтому повторная или встречная заявка (последовательная
 * или одновременная) завершается {@link FriendshipOfferAlreadyExistsException}. Хранилище пропускает такую
 * вставку без ошибки БД, транзакция остаётся рабочей, и сервис читает существующую заявку и бросает ошибку
 * её статуса: {@link FriendshipOfferAlreadyPendingException}, {@link FriendshipOfferAlreadyAcceptedException}
 * или {@link FriendshipOfferAlreadyRejectedException}; затем транзакция откатывается. Подписка отправителя
 * вставляется с {@code ON CONFLICT DO NOTHING}, поэтому подписка, созданная одновременно (например, принятием
 * встречной заявки), не ошибка.
 *
 * <p>Правила новой заявки (id заданы, не самому себе, {@code PENDING}) проверяет {@link FriendshipOffer#create}
 * до обращения к БД.
 */
@Service
@RequiredArgsConstructor
public class CreateFriendshipOffer {

    /** Хранилище заявок в друзья. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Подписывает отправителя на получателя (api модуля subscriptions). */
    private final Subscriptions subscriptions;

    /** Преобразует доменную заявку в запрос порта, а ответ порта — в результат сценария. */
    private final FriendshipOfferApplicationMapper mapper;

    /** Источник времени создания заявок и подписок. */
    private final Clock clock;

    /**
     * Создаёт заявку в друзья в статусе {@code PENDING} и подписывает отправителя на получателя,
     * если он ещё не подписан.
     *
     * @param command отправитель и получатель заявки
     * @return созданная заявка
     * @throws FriendshipOfferToSelfException          пользователь предлагает дружбу самому себе
     * @throws FriendshipOfferAlreadyPendingException  заявка этой пары (в любом направлении) ждёт решения
     * @throws FriendshipOfferAlreadyAcceptedException заявка этой пары (в любом направлении) уже принята
     * @throws FriendshipOfferAlreadyRejectedException заявка этой пары (в любом направлении) уже отклонена
     * @throws FriendshipOfferAlreadyExistsException   заявка этой пары есть, но прочитать её не удалось
     */
    @Transactional
    public CreateFriendshipOfferResult create(CreateFriendshipOfferCommand command) {
        FriendshipOffer offer = FriendshipOffer.create(command.fromUserId(), command.toUserId(), clock.instant());
        CreateFriendshipOfferResponse created;
        try {
            created = friendshipOfferRepository.createFriendshipOffer(mapper.toRequest(offer));
        } catch (FriendshipOfferAlreadyExistsException e) {
            throw alreadyExistsError(offer, e);
        }
        subscriptions.subscribeIfAbsent(offer.fromUserId(), offer.toUserId(), offer.createdAt());
        return mapper.toResult(created);
    }

    /**
     * Читает заявку пары, из-за которой вставка не прошла (это может быть встречная заявка), и выбирает
     * ошибку её статуса; ошибка содержит настоящее направление этой заявки. Если заявку прочитать не удалось,
     * возвращается исходная ошибка дубликата.
     *
     * @param offer заявка, которую не удалось вставить
     * @param cause сообщение хранилища о том, что заявка пары уже есть
     * @return ошибка, которую нужно бросить
     */
    private RuntimeException alreadyExistsError(
            FriendshipOffer offer, FriendshipOfferAlreadyExistsException cause
    ) {
        return friendshipOfferRepository.findByPair(offer.fromUserId(), offer.toUserId())
                .map(existing -> switch (existing.status()) {
                    case PENDING -> new FriendshipOfferAlreadyPendingException(
                            existing.id(), existing.fromUserId(), existing.toUserId()
                    );
                    case ACCEPTED -> new FriendshipOfferAlreadyAcceptedException(
                            existing.id(), existing.fromUserId(), existing.toUserId()
                    );
                    case REJECTED -> new FriendshipOfferAlreadyRejectedException(
                            existing.id(), existing.fromUserId(), existing.toUserId()
                    );
                })
                .orElse(cause);
    }
}
