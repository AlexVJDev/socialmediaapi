package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferDirection;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipOfferRepository {

    /** Максимальный размер страницы {@link #findPage} и {@link #findPageAfter}. */
    int MAX_LIMIT = 100;

    /**
     * Сохраняет заявку. У пары пользователей не больше одной заявки в любом направлении,
     * поэтому повторная или встречная заявка — дубликат. Дубликат не прерывает транзакцию вызывающего:
     * вставка пропускается без ошибки БД, и вызывающий может прочитать существующую заявку.
     *
     * @param request заявка для сохранения
     * @return сохранённая заявка
     * @throws FriendshipOfferAlreadyExistsException заявка этой пары пользователей уже есть
     *                                               (в том же или обратном направлении)
     */
    CreateFriendshipOfferResponse createFriendshipOffer(
            CreateFriendshipOfferRequest request
    );

    /**
     * Находит заявку пары пользователей в любом направлении и блокирует её до конца транзакции.
     *
     * @param userA один пользователь пары
     * @param userB другой пользователь пары
     * @return заявка от {@code userA} к {@code userB} или от {@code userB} к {@code userA};
     *         пусто, если у пары нет заявки
     */
    Optional<FindFriendshipOfferResponse> findByPair(UUID userA, UUID userB);

    /**
     * Сохраняет решение по заявке одним запросом: статус меняется, только если заявка
     * всё ещё {@code PENDING} и {@code request.actorId()} — её получатель. Из одновременных
     * решений по одной заявке успешно ровно одно, остальные получают пустой результат.
     *
     * @param request заявка, решающий пользователь, новый статус и время решения
     * @return заявка с новым статусом; пусто, если заявки нет, пользователь не её получатель
     *         или заявка уже не {@code PENDING} (случаи не различаются: чтобы выбрать ошибку,
     *         прочитайте заявку через {@link #findByIdAndRecipient})
     */
    Optional<UpdateFriendshipOfferStatusResponse> updateStatusIfPending(UpdateFriendshipOfferStatusRequest request);

    /**
     * Читает заявку, чтобы выбрать ошибку, после того как {@link #updateStatusIfPending} вернул пусто;
     * не использовать для решения, писать ли.
     *
     * @param offerId     id заявки
     * @param recipientId ожидаемый получатель заявки
     * @return заявка; пусто, если заявки нет или её получатель не {@code recipientId}
     */
    Optional<FindFriendshipOfferResponse> findByIdAndRecipient(UUID offerId, UUID recipientId);

    /**
     * Читает заявку в любом статусе, без блокировки.
     *
     * @param offerId id заявки
     * @return заявка; пусто, если заявки нет
     */
    Optional<FindFriendshipOfferResponse> findById(UUID offerId);

    /**
     * Первая страница заявок {@code userId} в заданном направлении и статусе, сначала новые
     * ({@code created_at DESC, id DESC}). Заявки других пользователей не читаются.
     *
     * @param userId    пользователь, чьи заявки читаются
     * @param direction {@code INCOMING} — {@code userId} получатель, {@code OUTGOING} — отправитель
     * @param status    статус заявок
     * @param limit     максимальное число заявок на странице, от 1 до {@link #MAX_LIMIT}
     * @return страница; её {@code next} пуст, если заявок больше нет
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     * @throws NullPointerException     {@code userId}, {@code direction} или {@code status} — {@code null}
     */
    FriendshipOfferPageResponse findPage(
            UUID userId,
            FriendshipOfferDirection direction,
            FriendshipOfferStatus status,
            int limit
    );

    /**
     * Страница тех же заявок, что и {@link #findPage}, строго после {@code after} в том же порядке.
     * Заявки, созданные после чтения первой страницы, новее любого курсора и на следующие
     * страницы не попадают.
     *
     * @param userId    пользователь, чьи заявки читаются
     * @param direction {@code INCOMING} — {@code userId} получатель, {@code OUTGOING} — отправитель
     * @param status    статус заявок
     * @param after     позиция последней показанной заявки из {@link FriendshipOfferPageResponse#next()}
     * @param limit     максимальное число заявок на странице, от 1 до {@link #MAX_LIMIT}
     * @return страница; её {@code next} пуст, если заявок больше нет
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     * @throws NullPointerException     {@code userId}, {@code direction}, {@code status} или {@code after}
     *                                  — {@code null}
     */
    FriendshipOfferPageResponse findPageAfter(
            UUID userId,
            FriendshipOfferDirection direction,
            FriendshipOfferStatus status,
            FriendshipOfferCursor after,
            int limit
    );
}
