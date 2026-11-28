package be.appify.prefab.processor.dbmigration;

/** Signals a failure while interpreting or replaying an existing migration statement. */
final class MigrationSqlException extends RuntimeException {
    enum Kind {
        REPLAY_STATE,
        UNSUPPORTED_SQL
    }

    private final Kind kind;

    private MigrationSqlException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    static MigrationSqlException replayState(String statement, Throwable cause) {
        return new MigrationSqlException(
                Kind.REPLAY_STATE,
                "statement '%s' failed: %s".formatted(statement, cause.getMessage()),
                cause);
    }

    static MigrationSqlException unsupportedSql(String statement, Throwable cause) {
        return new MigrationSqlException(
                Kind.UNSUPPORTED_SQL,
                "statement '%s' cannot be parsed or modeled: %s".formatted(statement, cause.getMessage()),
                cause);
    }

    Kind kind() {
        return kind;
    }
}
