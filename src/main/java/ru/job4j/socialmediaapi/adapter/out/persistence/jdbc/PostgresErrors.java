package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

/**
 * Reads the details PostgreSQL reports for a violated constraint,
 * so that adapters can translate it into the application's own exceptions.
 */
final class PostgresErrors {

    private PostgresErrors() {
    }

    /**
     * @return name of the violated constraint, or an empty string
     *         when PostgreSQL did not report one (e.g. NOT NULL)
     */
    static String constraint(DataIntegrityViolationException exception) {
        return serverError(exception)
                .map(ServerErrorMessage::getConstraint)
                .orElse("");
    }

    /**
     * @return column of the violated NOT NULL constraint, if that is the cause
     */
    static Optional<String> notNullColumn(DataIntegrityViolationException exception) {
        return serverError(exception)
                .filter(error -> PSQLState.NOT_NULL_VIOLATION.getState().equals(error.getSQLState()))
                .map(ServerErrorMessage::getColumn);
    }

    private static Optional<ServerErrorMessage> serverError(
            DataIntegrityViolationException exception
    ) {
        return exception.getMostSpecificCause() instanceof PSQLException psql
                ? Optional.ofNullable(psql.getServerErrorMessage())
                : Optional.empty();
    }
}
