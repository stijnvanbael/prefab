package be.appify.prefab.processor.dbmigration;

/** Signals that an existing migration file contains SQL that Prefab cannot interpret. */
final class MigrationSqlException extends RuntimeException {
    MigrationSqlException(String message, Throwable cause) {
        super(message, cause);
    }
}
