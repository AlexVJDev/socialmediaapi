package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.OfferFriendshipEntity;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.OfferFriendshipPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository;

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

    private final OfferFriendshipPersistenceMapper mapper;

    public OfferFriendshipRepositoryJdbc(
            JdbcTemplate jdbcTemplate,
            OfferFriendshipPersistenceMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    public CreateOfferFriendshipResponse createOfferFriendship(
            CreateOfferFriendshipRequest request
    ) {
        var entity = mapper.toEntity(request);
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
        return mapper.toResponse(saved);
    }
}
