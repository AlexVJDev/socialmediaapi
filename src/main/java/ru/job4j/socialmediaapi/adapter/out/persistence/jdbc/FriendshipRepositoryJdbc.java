package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.FriendshipEntity;
import ru.job4j.socialmediaapi.application.port.out.FriendshipRepository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class FriendshipRepositoryJdbc implements FriendshipRepository {

    private static final String CREATE = """
            INSERT INTO friendships (
                id, first_user_id, second_user_id,
                offer_friendship_id, created_at
            )
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, first_user_id, second_user_id,
                      offer_friendship_id, created_at
            """;

    private static final RowMapper<FriendshipEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new FriendshipEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("first_user_id", UUID.class),
                    resultSet.getObject("second_user_id", UUID.class),
                    resultSet.getObject("offer_friendship_id", UUID.class),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    private final JdbcTemplate jdbcTemplate;

    public FriendshipRepositoryJdbc(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    ) {
        var entity = toEntity(request);
        var saved = jdbcTemplate.queryForObject(
                CREATE,
                ENTITY_MAPPER,
                entity.id(),
                entity.firstUserId(),
                entity.secondUserId(),
                entity.offerFriendshipId(),
                Timestamp.from(entity.createdAt())
        );
        return toResponse(saved);
    }

    private static FriendshipEntity toEntity(
            CreateFriendshipRequest request
    ) {
        return new FriendshipEntity(
                request.id(),
                request.firstUserId(),
                request.secondUserId(),
                request.offerFriendshipId(),
                request.createdAt()
        );
    }

    private static CreateFriendshipResponse toResponse(
            FriendshipEntity entity
    ) {
        return new CreateFriendshipResponse(
                entity.id(),
                entity.firstUserId(),
                entity.secondUserId(),
                entity.offerFriendshipId(),
                entity.createdAt()
        );
    }
}
