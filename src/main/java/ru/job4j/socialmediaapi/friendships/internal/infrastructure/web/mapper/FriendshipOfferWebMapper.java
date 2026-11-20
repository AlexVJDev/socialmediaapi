package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOffersCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferDirection;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.FriendshipOfferDirectionWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.FriendshipOfferStatusWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.RejectFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferPageWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;

import java.time.Instant;
import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Преобразования между web DTO {@code FriendshipOfferController} и сценариями.
 * <p>
 * Методы {@code to*Response} играют роль Presenter: знают форму внешнего ответа
 * и избавляют контроллер от деталей его сборки (маппинг enum, переименование полей,
 * заполнение DTO). Маппер не содержит бизнес-правил и не вызывает репозитории.
 * Методы {@code to*Command} работают в обратную сторону: web-запрос → команда сценария.
 * <p>
 * Web DTO генерируются по OpenAPI-контракту ({@code openapi-generator-maven-plugin}).
 * {@code status} маппится по имени: доменный {@code FriendshipOfferStatus} → {@code StatusEnum} ответа.
 * Ответы create, accept и reject допускают только статус, к которому ведёт операция
 * ({@code PENDING}, {@code ACCEPTED}, {@code REJECTED}); любой другой — ошибка в коде, и вместо
 * ответа, нарушающего контракт, выбрасывается {@link IllegalArgumentException}.
 * Время решения accept/reject названо по эндпоинту: {@code acceptedAt} / {@code rejectedAt}.
 * Query- и path-параметры GET-эндпоинтов приходят отдельными аргументами сгенерированного интерфейса.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferWebMapper {

    CreateFriendshipOfferCommand toCommand(CreateFriendshipOfferWebRequest request);

    CreateFriendshipOfferWebResponse toResponse(CreateFriendshipOfferResult result);

    AcceptFriendshipOfferCommand toAcceptCommand(AcceptFriendshipOfferWebRequest request);

    RejectFriendshipOfferCommand toRejectCommand(RejectFriendshipOfferWebRequest request);

    @Mapping(target = "acceptedAt", source = "decidedAt")
    AcceptFriendshipOfferWebResponse toAcceptResponse(FriendshipOfferDecisionResult result);

    @Mapping(target = "rejectedAt", source = "decidedAt")
    RejectFriendshipOfferWebResponse toRejectResponse(FriendshipOfferDecisionResult result);

    @Mapping(target = "offerId", source = "offerId")
    GetFriendshipOfferCommand toGetCommand(UUID offerId);

    /**
     * @param userId         текущий пользователь из {@code CurrentUserProvider}
     * @param direction      направление заявок; обязательно по контракту, поэтому не {@code null}
     * @param status         статус заявок; значение по умолчанию из контракта ({@code PENDING}) подставляет Spring
     * @param limit          размер страницы; значение по умолчанию (20) подставляет Spring, поэтому не {@code null}
     * @param afterCreatedAt {@code next.createdAt} предыдущей страницы, {@code null} для первой страницы
     * @param afterId        {@code next.id} предыдущей страницы, {@code null} для первой страницы
     * @return команда сценария
     */
    GetFriendshipOffersCommand toGetPageCommand(
            UUID userId,
            FriendshipOfferDirectionWebRequest direction,
            FriendshipOfferStatusWebRequest status,
            Integer limit,
            Instant afterCreatedAt,
            UUID afterId
    );

    /**
     * @param direction направление из query-параметра
     * @return то же направление порта, по имени
     */
    FriendshipOfferDirection toDirection(FriendshipOfferDirectionWebRequest direction);

    /**
     * @param status статус из query-параметра
     * @return тот же доменный статус, по имени
     */
    FriendshipOfferStatus toStatus(FriendshipOfferStatusWebRequest status);

    FriendshipOfferWebResponse toResponse(FriendshipOfferResult result);

    FriendshipOfferPageWebResponse toResponse(FriendshipOfferPageResult result);

    /**
     * @param status статус созданной заявки
     * @return {@code PENDING}; на любой другой статус — {@link IllegalArgumentException}
     */
    @ValueMapping(source = "PENDING", target = "PENDING")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    CreateFriendshipOfferWebResponse.StatusEnum toCreateStatus(FriendshipOfferStatus status);

    /**
     * @param status статус принятой заявки
     * @return {@code ACCEPTED}; на любой другой статус — {@link IllegalArgumentException}
     */
    @ValueMapping(source = "ACCEPTED", target = "ACCEPTED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    AcceptFriendshipOfferWebResponse.StatusEnum toAcceptStatus(FriendshipOfferStatus status);

    /**
     * @param status статус отклонённой заявки
     * @return {@code REJECTED}; на любой другой статус — {@link IllegalArgumentException}
     */
    @ValueMapping(source = "REJECTED", target = "REJECTED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    RejectFriendshipOfferWebResponse.StatusEnum toRejectStatus(FriendshipOfferStatus status);
}
