package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

/**
 * Reads the details PostgreSQL reports for a violated constraint,
 * so that adapters can translate a duplicate (unique key violation)
 * into the application's own exception.
 */
final class PostgresErrors {

    private PostgresErrors() {
    }

    /**
     * @param exception exception thrown by Spring JDBC
     * @return name of the violated constraint, or an empty string
     *         when PostgreSQL did not report one (e.g. NOT NULL)
     */
    static String constraint(DataIntegrityViolationException exception) {
        return serverError(exception)
                .map(ServerErrorMessage::getConstraint)
                .orElse("");
    }

    private static Optional<ServerErrorMessage> serverError(
            DataIntegrityViolationException exception
    ) {
        return exception.getMostSpecificCause() instanceof PSQLException psql
                ? Optional.ofNullable(psql.getServerErrorMessage())
                : Optional.empty();
    }
}
