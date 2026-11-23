package be.appify.prefab.streams.postgres;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Creates the {@code prefab_stream_*} tables. The DDL is idempotent. */
public final class PostgresStreamSchema {
    /** Classpath location of the DDL, published in the developer guide. */
    public static final String DDL_RESOURCE = "prefab-streams-postgres-schema.sql";

    private PostgresStreamSchema() {
    }

    /**
     * Returns the DDL text.
     *
     * @return the SQL script
     */
    public static String ddl() {
        try (var in = new ClassPathResource(DDL_RESOURCE).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + DDL_RESOURCE, e);
        }
    }

    /**
     * Runs the DDL.
     *
     * @param jdbc the template of the target database
     */
    public static void initialize(JdbcTemplate jdbc) {
        for (var statement : ddl().replaceAll("(?m)^--.*$", "").split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement.strip());
            }
        }
    }
}
