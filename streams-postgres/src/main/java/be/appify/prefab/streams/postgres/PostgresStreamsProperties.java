package be.appify.prefab.streams.postgres;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the Postgres stream state repository.
 *
 * @param initializeSchema whether the {@code prefab_stream_*} tables are created on startup
 */
@ConfigurationProperties("prefab.streams.state")
public record PostgresStreamsProperties(InitializeSchema initializeSchema) {
    /** Creates properties, defaulting to {@link InitializeSchema#ALWAYS}. */
    public PostgresStreamsProperties {
        initializeSchema = initializeSchema == null ? InitializeSchema.ALWAYS : initializeSchema;
    }

    /** Schema initialization mode, like Spring Batch's. */
    public enum InitializeSchema {
        /** Run the idempotent DDL on every start. */
        ALWAYS,
        /** Never touch the schema; the application manages the tables. */
        NEVER
    }
}
