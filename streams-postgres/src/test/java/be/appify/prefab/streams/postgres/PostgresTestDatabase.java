package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Postgres for the tests: a Testcontainers instance, or the database named by the
 * {@code PREFAB_TEST_POSTGRES_URL} environment variable (with {@code PREFAB_TEST_POSTGRES_USER} and
 * {@code PREFAB_TEST_POSTGRES_PASSWORD}) where Docker is unavailable.
 */
final class PostgresTestDatabase {
    private static final PostgreSQLContainer CONTAINER = startContainerUnlessExternal();
    private static final DataSource DATA_SOURCE = newDataSource();

    private PostgresTestDatabase() {
    }

    static PostgresStreamStateRepository freshRepository() {
        var jdbc = new JdbcTemplate(DATA_SOURCE);
        jdbc.execute("DROP TABLE IF EXISTS prefab_stream_store, prefab_stream_inbox, "
                + "prefab_stream_outbox, prefab_stream_lease, prefab_stream_schedule, "
                + "prefab_stream_member, prefab_stream_member_group");
        PostgresStreamSchema.initialize(jdbc);
        return new PostgresStreamStateRepository(
                jdbc, new DataSourceTransactionManager(DATA_SOURCE), new JsonStreamStateCodec());
    }

    static JdbcTemplate jdbc() {
        return new JdbcTemplate(DATA_SOURCE);
    }

    /** Returns a new {@link DataSource} object for the shared database, distinct from the one the repositories use. */
    static DataSource newDataSource() {
        var source = new PGSimpleDataSource();
        if (CONTAINER == null) {
            source.setUrl(System.getenv("PREFAB_TEST_POSTGRES_URL"));
            source.setUser(System.getenv("PREFAB_TEST_POSTGRES_USER"));
            source.setPassword(System.getenv("PREFAB_TEST_POSTGRES_PASSWORD"));
        } else {
            source.setUrl(CONTAINER.getJdbcUrl());
            source.setUser(CONTAINER.getUsername());
            source.setPassword(CONTAINER.getPassword());
        }
        return source;
    }

    static PostgresStreamStateRepository newInstance() {
        return new PostgresStreamStateRepository(
                new JdbcTemplate(DATA_SOURCE), new DataSourceTransactionManager(DATA_SOURCE), new JsonStreamStateCodec());
    }

    private static PostgreSQLContainer startContainerUnlessExternal() {
        if (System.getenv("PREFAB_TEST_POSTGRES_URL") != null) {
            return null;
        }
        var container = new PostgreSQLContainer("postgres:16-alpine");
        container.start();
        return container;
    }
}
