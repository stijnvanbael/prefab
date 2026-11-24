package be.appify.prefab.streams.mongodb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the MongoDB stream state repository.
 *
 * @param initializeSchema whether the {@code prefab_stream_*} collections and indexes are created on startup
 */
@ConfigurationProperties("prefab.streams.state")
public record MongoStreamsProperties(InitializeSchema initializeSchema) {
    /** Creates properties, defaulting to {@link InitializeSchema#ALWAYS}. */
    public MongoStreamsProperties {
        initializeSchema = initializeSchema == null ? InitializeSchema.ALWAYS : initializeSchema;
    }

    /** Schema initialization mode, like Spring Batch's. */
    public enum InitializeSchema {
        /** Create the missing collections and indexes on every start. */
        ALWAYS,
        /** Never touch the schema; the application manages the collections and indexes. */
        NEVER
    }
}
