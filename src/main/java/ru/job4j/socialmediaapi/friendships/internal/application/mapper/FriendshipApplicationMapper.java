package ru.job4j.socialmediaapi.friendships.internal.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendCursorResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.Friendship;

import java.util.Optional;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/** Доменная дружба → запрос порта {@code FriendshipsRepository}; страница друзей порта → результат. */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipApplicationMapper {

    CreateFriendshipRequest toRequest(Friendship friendship);

    FriendResult toResult(FindFriendResponse response);

    FriendPageResult toResult(FriendPageResponse response);

    FriendCursorResult toCursorResult(FriendCursor cursor);

    /**
     * @param next курсор следующей страницы, пустой, если друзей больше нет
     * @return курсор или {@code null}, если друзей больше нет
     */
    default FriendCursorResult unwrapNext(Optional<FriendCursor> next) {
        return next.map(this::toCursorResult).orElse(null);
    }
}
