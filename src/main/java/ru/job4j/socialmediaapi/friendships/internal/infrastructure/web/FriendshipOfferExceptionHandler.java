package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;
import ru.job4j.socialmediaapi.shared.web.CommonErrorCode;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.error.FriendshipOfferErrorCode;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendCursorException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendshipOfferCursorException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;

/**
 * Ошибки эндпоинтов модуля (заявки и друзья) → HTTP-ответы API-контракта.
 * Действует только для {@link FriendshipOfferController} и {@link FriendshipController}; срабатывает
 * раньше общего {@code shared.exception.GlobalExceptionHandler}, который отвечает на неожиданные ошибки {@code 500}.
 */
@RestControllerAdvice(assignableTypes = {FriendshipOfferController.class, FriendshipController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FriendshipOfferExceptionHandler {

    /** Логирует дубликат, статус которого не удалось прочитать. */
    private static final Logger LOG = LoggerFactory.getLogger(FriendshipOfferExceptionHandler.class);

    /**
     * Некорректный запрос к /api/v1/friendship-offer[/*] и /api/v1/friendships: один общий код VALIDATION_ERROR
     * для всех случаев.
     * <ul>
     *     <li>тело create, accept, reject: не читается, невалидно или заявка самому себе;</li>
     *     <li>path- и query-параметры: отсутствуют ({@code direction}, {@code userId}) или неверного типа
     *     (id не UUID, {@code direction} или {@code status} не из списка, {@code limit} или {@code afterCreatedAt}
     *     не преобразуются);</li>
     *     <li>query-параметры страниц заявок и друзей: {@code limit} вне 1..100
     *     ({@link ConstraintViolationException} от {@code @Validated}-интерфейса),
     *     задана только одна часть курсора страницы.</li>
     * </ul>
     *
     * @return {@code 400} с телом ошибки VALIDATION_ERROR
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            FriendshipOfferToSelfException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            IncompleteFriendshipOfferCursorException.class,
            IncompleteFriendCursorException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest() {
        return CommonErrorCode.VALIDATION_ERROR.response();
    }

    /**
     * Повторная или встречная заявка, пока заявка пары ждёт решения:
     * ничего не изменилось.
     *
     * @return {@code 200} с телом FRIENDSHIP_OFFER_ALREADY_PENDING
     */
    @ExceptionHandler(FriendshipOfferAlreadyPendingException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyPending() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.response();
    }

    /**
     * Принятие/отклонение заявки, которой нет или получатель которой не тот, кто действует
     * (чужая заявка не раскрывается); чтение несуществующей заявки.
     *
     * @return {@code 404} с телом ошибки FRIENDSHIP_OFFER_NOT_FOUND
     */
    @ExceptionHandler(FriendshipRequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_NOT_FOUND.response();
    }

    @ExceptionHandler(FriendshipOfferAlreadyAcceptedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyAccepted() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_ACCEPTED.response();
    }

    @ExceptionHandler(FriendshipOfferAlreadyRejectedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyRejected() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_REJECTED.response();
    }

    /**
     * Сервис пробрасывает дубликат, только если существующую заявку не удалось прочитать
     * после нарушения уникального ключа (см. {@code CreateFriendshipOffer}): заявка есть,
     * но её статус неизвестен, поэтому клиент получает конфликт: заявка пары уже существует.
     * Случай логируется, потому что его не должно быть.
     *
     * @param exception дубликат, о котором сообщило хранилище
     * @return {@code 409} с телом ошибки FRIENDSHIP_OFFER_ALREADY_EXISTS
     */
    @ExceptionHandler(FriendshipOfferAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(FriendshipOfferAlreadyExistsException exception) {
        LOG.warn("Friendship offer already exists but its status could not be read: {}",
                exception.getMessage(), exception);
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_EXISTS.response();
    }
}
