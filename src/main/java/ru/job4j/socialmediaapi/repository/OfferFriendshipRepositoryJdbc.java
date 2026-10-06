package ru.job4j.socialmediaapi.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.domain.OfferFriendshipStatus;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class OfferFriendshipRepositoryJdbc implements OfferFriendshipRepository {

    private static final String CREATE = """
            INSERT INTO offer_friendships (
                id, from_user_id, to_user_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, from_user_id, to_user_id,
                      status, created_at, updated_at
            """;

    private static final RowMapper<OfferFriendshipEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new OfferFriendshipEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("from_user_id", UUID.class),
                    resultSet.getObject("to_user_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getTimestamp("updated_at").toInstant()
            );

    private final JdbcTemplate jdbcTemplate;

    public OfferFriendshipRepositoryJdbc(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CreateOfferFriendshipResponse createOfferFriendship(
            CreateOfferFriendshipRequest request
    ) {
        var entity = toEntity(request);
        var saved = jdbcTemplate.queryForObject(
                CREATE,
                ENTITY_MAPPER,
                entity.id(),
                entity.fromUserId(),
                entity.toUserId(),
                entity.status(),
                Timestamp.from(entity.createdAt()),
                Timestamp.from(entity.updatedAt())
        );
        return toResponse(saved);
    }

    private static OfferFriendshipEntity toEntity(
            CreateOfferFriendshipRequest request
    ) {
        return new OfferFriendshipEntity(
                request.id(),
                request.fromUserId(),
                request.toUserId(),
                request.status().name(),
                request.createdAt(),
                request.updatedAt()
        );
    }

    private static CreateOfferFriendshipResponse toResponse(
            OfferFriendshipEntity entity
    ) {
        return new CreateOfferFriendshipResponse(
                entity.id(),
                entity.fromUserId(),
                entity.toUserId(),
                OfferFriendshipStatus.valueOf(entity.status()),
                entity.createdAt(),
                entity.updatedAt()
        );
    }
}
