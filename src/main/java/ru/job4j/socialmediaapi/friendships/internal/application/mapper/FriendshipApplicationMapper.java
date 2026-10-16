package ru.job4j.socialmediaapi.friendships.internal.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.Friendship;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/** Domain friendship → request of the {@code FriendshipsRepository} port. */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipApplicationMapper {

    CreateFriendshipRequest toRequest(Friendship friendship);
}
