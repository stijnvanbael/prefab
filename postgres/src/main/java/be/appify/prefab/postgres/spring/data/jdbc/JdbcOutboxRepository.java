package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.outbox.OutboxEntry;
import be.appify.prefab.core.outbox.OutboxRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * JDBC-based implementation of {@link OutboxRepository}.
 * <p>
 * Persists outbox entries to the {@code prefab_outbox} table within the same transaction
 * as the aggregate state change, guaranteeing at-least-once delivery semantics.
 * </p>
 */
public class JdbcOutboxRepository implements OutboxRepository {

    private static final String INSERT_SQL =
            "INSERT INTO prefab_outbox (id, aggregate_type, aggregate_id, event_type, payload, created_at, published_at)"
            + " VALUES (:id, :aggregateType, :aggregateId, :eventType, :payload, :createdAt, NULL)";

    private static final String SELECT_PENDING_SQL =
            "SELECT id, aggregate_type, aggregate_id, event_type, payload, created_at, published_at"
            + " FROM prefab_outbox WHERE published_at IS NULL ORDER BY created_at LIMIT :batchSize"
            + " FOR UPDATE SKIP LOCKED";

    private static final String DELETE_SQL = "DELETE FROM prefab_outbox WHERE id = :id";

    private final NamedParameterJdbcOperations jdbcOperations;

    /**
     * Constructs a new JdbcOutboxRepository.
     *
     * @param jdbcOperations the named-parameter JDBC operations to use
     */
    public JdbcOutboxRepository(NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public void save(OutboxEntry entry) {
        jdbcOperations.update(INSERT_SQL, new MapSqlParameterSource()
                .addValue("id", entry.id())
                .addValue("aggregateType", entry.aggregateType())
                .addValue("aggregateId", entry.aggregateId())
                .addValue("eventType", entry.eventType())
                .addValue("payload", entry.payload())
                .addValue("createdAt", Timestamp.from(entry.createdAt())));
    }

    @Override
    public List<OutboxEntry> findPending(int batchSize) {
        return jdbcOperations.query(SELECT_PENDING_SQL, Map.of("batchSize", batchSize),
                (rs, rowNum) -> new OutboxEntry(
                        rs.getString("id"),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("published_at") != null
                                ? rs.getTimestamp("published_at").toInstant()
                                : null
                ));
    }

    @Override
    public void delete(String id) {
        jdbcOperations.update(DELETE_SQL, Map.of("id", id));
    }
}
