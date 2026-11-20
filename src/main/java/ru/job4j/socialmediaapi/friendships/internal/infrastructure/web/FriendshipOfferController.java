package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.AcceptFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.CreateFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.GetFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.GetFriendshipOffers;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.RejectFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper.FriendshipOfferWebMapper;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.FriendshipOfferDirectionWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.FriendshipOfferStatusWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.RejectFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.shared.security.CurrentUserProvider;

import java.time.Instant;
import java.util.UUID;

/**
 * Эндпоинты {@code /api/v1/friendship-offer} и {@code /api/v1/friendship-offer/*}.
 * Пути, HTTP-методы, параметры и их валидация берутся из {@link FriendshipOfferApi},
 * сгенерированного по OpenAPI-контракту ({@code docs/api/paths/friendship-offer*.yaml});
 * аннотации здесь не повторяются. Методы возвращают {@code ResponseEntity<?>}, как и интерфейс:
 * тип тела зависит от исхода (заявка или, из обработчика исключений, тело ошибки).
 */
@RestController
@RequiredArgsConstructor
public class FriendshipOfferController implements FriendshipOfferApi {

    /** Сценарий создания заявки в друзья. */
    private final CreateFriendshipOffer createFriendshipOffer;

    /** Сценарий принятия заявки в друзья. */
    private final AcceptFriendshipOffer acceptFriendshipOffer;

    /** Сценарий отклонения заявки в друзья. */
    private final RejectFriendshipOffer rejectFriendshipOffer;

    /** Сценарий чтения одной заявки. */
    private final GetFriendshipOffer getFriendshipOffer;

    /** Сценарий чтения страницы заявок текущего пользователя. */
    private final GetFriendshipOffers getFriendshipOffers;

    /** Текущий пользователь: читатель страницы заявок. */
    private final CurrentUserProvider currentUser;

    /** Преобразует web DTO в команду сценария и результат обратно. */
    private final FriendshipOfferWebMapper mapper;

    /**
     * {@code 201} с новой заявкой. На повторную или встречную заявку отвечает
     * {@code FriendshipOfferExceptionHandler} по статусу заявки пары:
     * {@code PENDING} → {@code 200} FRIENDSHIP_OFFER_ALREADY_PENDING,
     * {@code ACCEPTED} / {@code REJECTED} → {@code 409} с кодом статуса,
     * статус прочитать не удалось → {@code 409} FRIENDSHIP_OFFER_ALREADY_EXISTS.
     *
     * @param request отправитель и получатель заявки
     * @return созданная заявка
     */
    @Override
    public ResponseEntity<?> createFriendshipOffer(
            CreateFriendshipOfferWebRequest request
    ) {
        CreateFriendshipOfferResult result = createFriendshipOffer.create(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }

    /**
     * {@code 200} с принятой заявкой; дружба и обратная подписка создаются
     * в той же транзакции. На ошибки ({@code 404} — заявки нет или пользователь не её получатель,
     * {@code 409} — уже решена) отвечает {@code FriendshipOfferExceptionHandler}.
     *
     * @param request заявка и принимающий её пользователь
     * @return принятая заявка
     */
    @Override
    public ResponseEntity<?> acceptFriendshipOffer(
            AcceptFriendshipOfferWebRequest request
    ) {
        FriendshipOfferDecisionResult result = acceptFriendshipOffer.accept(mapper.toAcceptCommand(request));
        return ResponseEntity.ok(mapper.toAcceptResponse(result));
    }

    /**
     * {@code 200} с отклонённой заявкой; дружба не создаётся, отправитель остаётся подписанным.
     * На ошибки ({@code 404} — заявки нет или пользователь не её получатель, {@code 409} — уже решена)
     * отвечает {@code FriendshipOfferExceptionHandler}.
     *
     * @param request заявка и отклоняющий её пользователь
     * @return отклонённая заявка
     */
    @Override
    public ResponseEntity<?> rejectFriendshipOffer(
            RejectFriendshipOfferWebRequest request
    ) {
        FriendshipOfferDecisionResult result = rejectFriendshipOffer.reject(mapper.toRejectCommand(request));
        return ResponseEntity.ok(mapper.toRejectResponse(result));
    }

    /**
     * {@code 200} с заявкой в любом статусе; на {@code 404} (заявки нет) и {@code 400}
     * (id не UUID) отвечает {@code FriendshipOfferExceptionHandler}.
     *
     * @param offerId id заявки
     * @return заявка
     */
    @Override
    public ResponseEntity<?> getFriendshipOffer(UUID offerId) {
        return ResponseEntity.ok(mapper.toResponse(getFriendshipOffer.get(mapper.toGetCommand(offerId))));
    }

    /**
     * {@code 200} со страницей заявок текущего пользователя, сначала новые: входящих
     * ({@code direction=INCOMING}) или исходящих ({@code OUTGOING}) в заданном статусе.
     * Следующая страница запрашивается с {@code afterCreatedAt} и {@code afterId} из {@code next};
     * {@code next == null} — заявок больше нет. На некорректные параметры ({@code 400}) отвечает
     * {@code FriendshipOfferExceptionHandler}; нет {@code X-User-Id} или не UUID — {@code 401}
     * ({@code GlobalExceptionHandler}).
     *
     * @param direction      {@code INCOMING} — текущий пользователь получатель, {@code OUTGOING} — отправитель
     * @param status         статус заявок ({@code PENDING}, если не задан)
     * @param limit          размер страницы, от 1 до 100 (20, если не задан)
     * @param afterCreatedAt {@code next.createdAt} предыдущей страницы
     * @param afterId        {@code next.id} предыдущей страницы
     * @return страница заявок
     */
    @Override
    public ResponseEntity<?> getFriendshipOffers(
            FriendshipOfferDirectionWebRequest direction,
            FriendshipOfferStatusWebRequest status,
            Integer limit,
            Instant afterCreatedAt,
            UUID afterId
    ) {
        UUID userId = currentUser.currentUserId();
        FriendshipOfferPageResult result = getFriendshipOffers.get(
                mapper.toGetPageCommand(userId, direction, status, limit, afterCreatedAt, afterId));
        return ResponseEntity.ok(mapper.toResponse(result));
    }
}
