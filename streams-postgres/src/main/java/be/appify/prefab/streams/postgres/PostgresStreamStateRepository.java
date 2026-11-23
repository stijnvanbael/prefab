package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamStateRepository;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link StreamStateRepository} on Postgres.
 *
 * <p>Reads happen while the engine runs a message. Changes are collected in the transaction and written in one
 * database transaction on {@link Transaction#commit()}: inbox identities, state rows guarded by a version, and
 * outbox rows. A stale version, a concurrently claimed inbox identity or a deadlock raises an
 * {@link OptimisticConflictException}, which the engine retries.
 */
public final class PostgresStreamStateRepository implements StreamStateRepository {
    private static final String SELECT_STATE =
            "SELECT state_value, version FROM prefab_stream_store WHERE namespace = ? AND state_key = ?";
    private static final String INSERT_STATE = """
            INSERT INTO prefab_stream_store (namespace, state_key, state_value, version, expiry_key, expires_at)
            VALUES (?, ?, ?, 1, ?, ?) ON CONFLICT (namespace, state_key) DO NOTHING""";
    private static final String UPDATE_STATE = """
            UPDATE prefab_stream_store
            SET state_value = ?, version = version + 1, expiry_key = ?, expires_at = ?
            WHERE namespace = ? AND state_key = ? AND version = ?""";
    private static final String DELETE_STATE =
            "DELETE FROM prefab_stream_store WHERE namespace = ? AND state_key = ? AND version = ?";
    private static final String SELECT_EXPIRED = """
            SELECT state_key, version FROM prefab_stream_store
            WHERE namespace = ? AND expiry_key = ? AND expires_at <= ?""";
    private static final String INSERT_INBOX = """
            INSERT INTO prefab_stream_inbox (subscription, message_id, recorded_at) VALUES (?, ?, ?)
            ON CONFLICT (subscription, message_id) DO NOTHING""";
    private static final String UPSERT_LEASE = """
            INSERT INTO prefab_stream_lease (topic, owner, expires_at) VALUES (?, ?, ?)
            ON CONFLICT (topic) DO UPDATE SET owner = EXCLUDED.owner, expires_at = EXCLUDED.expires_at
            WHERE prefab_stream_lease.owner = EXCLUDED.owner OR prefab_stream_lease.expires_at <= ?""";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final StreamStateCodec codec;

    /**
     * Creates a repository.
     *
     * @param jdbc the template of the database holding the {@code prefab_stream_*} tables
     * @param transactionManager the manager for that database, used for the per-message transaction
     * @param codec the value and key codec
     */
    public PostgresStreamStateRepository(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            StreamStateCodec codec
    ) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.transactions = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager must not be null"));
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
    }

    @Override
    public Transaction beginTransaction() {
        return new PostgresTransaction();
    }

    @Override
    public void cleanupExpired(Instant now, Duration inboxRetention) {
        if (now == null || inboxRetention == null || inboxRetention.isNegative()) {
            throw new IllegalArgumentException("now and non-negative inboxRetention are required");
        }
        jdbc.update("DELETE FROM prefab_stream_inbox WHERE recorded_at < ?",
                Timestamp.from(now.minus(inboxRetention)));
        jdbc.update("DELETE FROM prefab_stream_lease WHERE expires_at <= ?", Timestamp.from(now));
    }

    @Override
    public List<OutboxRecord> leaseOutbox(String topic, String owner, Instant now, Duration lease, int limit) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        if (lease == null || lease.isZero() || lease.isNegative()) {
            throw new IllegalArgumentException("lease must be positive");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        var claimed = jdbc.update(UPSERT_LEASE, topic, owner, Timestamp.from(now.plus(lease)), Timestamp.from(now));
        if (claimed == 0) {
            return List.of();
        }
        return jdbc.query(
                "SELECT id, topic, message FROM prefab_stream_outbox WHERE topic = ? ORDER BY seq LIMIT ?",
                (rs, row) -> new OutboxRecord(
                        rs.getString("id"), rs.getString("topic"), codec.decodeMessage(rs.getBytes("message"))),
                topic, limit);
    }

    @Override
    public void completeOutbox(String owner, String recordId) {
        var deleted = jdbc.update("""
                DELETE FROM prefab_stream_outbox o USING prefab_stream_lease l
                WHERE o.id = ? AND l.topic = o.topic AND l.owner = ? AND l.expires_at > ?""",
                recordId, owner, Timestamp.from(Instant.now()));
        if (deleted == 0 && outboxRecordExists(recordId)) {
            throw new IllegalStateException(
                    "Outbox record %s is not leased by %s or the topic lease expired".formatted(recordId, owner));
        }
    }

    private boolean outboxRecordExists(String recordId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM prefab_stream_outbox WHERE id = ?)", Boolean.class, recordId));
    }

    private final class PostgresTransaction implements Transaction {
        private final Map<StateKey, Long> observedVersions = new LinkedHashMap<>();
        private final Map<StateKey, Change> changes = new LinkedHashMap<>();
        private final Set<InboxKey> pendingInbox = new LinkedHashSet<>();
        private final List<OutboxRecord> pendingOutbox = new ArrayList<>();
        private boolean complete;

        @Override
        public boolean recordInbox(String subscription, String messageId) {
            ensureOpen();
            var key = new InboxKey(subscription, messageId);
            if (pendingInbox.contains(key) || Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM prefab_stream_inbox WHERE subscription = ? AND message_id = ?)",
                    Boolean.class, subscription, messageId))) {
                return false;
            }
            pendingInbox.add(key);
            return true;
        }

        @Override
        public Optional<Object> get(String namespace, Object key) {
            ensureOpen();
            var stateKey = stateKey(namespace, key);
            var change = changes.get(stateKey);
            if (change != null) {
                return change.deleted() ? Optional.empty() : Optional.ofNullable(change.value());
            }
            var rows = jdbc.query(SELECT_STATE,
                    (rs, row) -> new StoredState(rs.getBytes("state_value"), rs.getLong("version")),
                    namespace, stateKey.bytes());
            if (rows.isEmpty()) {
                observedVersions.putIfAbsent(stateKey, 0L);
                return Optional.empty();
            }
            observedVersions.putIfAbsent(stateKey, rows.getFirst().version());
            return Optional.ofNullable(codec.decodeValue(rows.getFirst().value()));
        }

        @Override
        public void put(String namespace, Object key, Object value) {
            write(namespace, key, new Change(value, false, null));
        }

        @Override
        public void putExpiring(String namespace, Object key, Object expiryKey, Object value, Instant expiresAt) {
            if (expiresAt == null) {
                throw new IllegalArgumentException("expiresAt must not be null");
            }
            write(namespace, key, new Change(value, false, new Expiration(codec.encodeKey(expiryKey), expiresAt)));
        }

        @Override
        public void delete(String namespace, Object key) {
            write(namespace, key, new Change(null, true, null));
        }

        private void write(String namespace, Object key, Change change) {
            ensureOpen();
            var stateKey = stateKey(namespace, key);
            if (!observedVersions.containsKey(stateKey) && !changes.containsKey(stateKey)) {
                get(namespace, key);
            }
            changes.put(stateKey, change);
        }

        @Override
        public void deleteExpired(String namespace, Object expiryKey, Instant streamTime) {
            ensureOpen();
            if (streamTime == null) {
                throw new IllegalArgumentException("streamTime must not be null");
            }
            var expiryBytes = codec.encodeKey(expiryKey);
            jdbc.query(SELECT_EXPIRED, (rs, row) -> new StoredKey(rs.getBytes("state_key"), rs.getLong("version")),
                            namespace, expiryBytes, Timestamp.from(streamTime))
                    .forEach(stored -> {
                        var stateKey = new StateKey(namespace, ByteBuffer.wrap(stored.key()));
                        observedVersions.putIfAbsent(stateKey, stored.version());
                        changes.put(stateKey, new Change(null, true, null));
                    });
            changes.entrySet().stream()
                    .filter(entry -> entry.getKey().namespace().equals(namespace))
                    .filter(entry -> !entry.getValue().deleted() && entry.getValue().expiration() != null)
                    .filter(entry -> entry.getValue().expiration().matches(expiryBytes, streamTime))
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(stateKey -> changes.put(stateKey, new Change(null, true, null)));
        }

        @Override
        public void enqueue(String topic, StreamMessage message) {
            ensureOpen();
            pendingOutbox.add(new OutboxRecord(UUID.randomUUID().toString(), topic, message));
        }

        @Override
        public void commit() {
            ensureOpen();
            try {
                transactions.executeWithoutResult(status -> apply());
            } catch (PessimisticLockingFailureException e) {
                throw new OptimisticConflictException("Deadlock while committing stream state: " + e.getMessage());
            } finally {
                complete = true;
            }
        }

        private void apply() {
            var now = Timestamp.from(Instant.now());
            for (var inbox : pendingInbox) {
                if (jdbc.update(INSERT_INBOX, inbox.subscription(), inbox.messageId(), now) == 0) {
                    throw new OptimisticConflictException("Inbox identity was concurrently claimed: " + inbox);
                }
            }
            changes.forEach(this::applyChange);
            for (var record : pendingOutbox) {
                jdbc.update("INSERT INTO prefab_stream_outbox (id, topic, message) VALUES (?, ?, ?)",
                        record.id(), record.topic(), codec.encodeMessage(record.message()));
            }
        }

        private void applyChange(StateKey key, Change change) {
            var expected = observedVersions.getOrDefault(key, 0L);
            var expiry = change.expiration();
            boolean changed;
            if (change.deleted()) {
                changed = expected == 0 || jdbc.update(DELETE_STATE, key.namespace(), key.bytes(), expected) == 1;
            } else if (expected == 0) {
                changed = jdbc.update(INSERT_STATE, key.namespace(), key.bytes(), codec.encodeValue(change.value()),
                        expiry == null ? null : expiry.key(),
                        expiry == null ? null : Timestamp.from(expiry.expiresAt())) == 1;
            } else {
                changed = jdbc.update(UPDATE_STATE, codec.encodeValue(change.value()),
                        expiry == null ? null : expiry.key(),
                        expiry == null ? null : Timestamp.from(expiry.expiresAt()),
                        key.namespace(), key.bytes(), expected) == 1;
            }
            if (!changed) {
                throw new OptimisticConflictException(
                        "State changed concurrently in namespace '%s'".formatted(key.namespace()));
            }
        }

        @Override
        public void rollback() {
            complete = true;
            changes.clear();
            pendingInbox.clear();
            pendingOutbox.clear();
        }

        private StateKey stateKey(String namespace, Object key) {
            return new StateKey(namespace, ByteBuffer.wrap(codec.encodeKey(key)));
        }

        private void ensureOpen() {
            if (complete) {
                throw new IllegalStateException("Transaction is already complete");
            }
        }
    }

    private record StateKey(String namespace, ByteBuffer key) {
        byte[] bytes() {
            return key.array();
        }
    }

    private record InboxKey(String subscription, String messageId) {
    }

    private record Change(Object value, boolean deleted, Expiration expiration) {
    }

    private record Expiration(byte[] key, Instant expiresAt) {
        boolean matches(byte[] expiryKey, Instant streamTime) {
            return Arrays.equals(key, expiryKey) && !expiresAt.isAfter(streamTime);
        }
    }

    private record StoredState(byte[] value, long version) {
    }

    private record StoredKey(byte[] key, long version) {
    }
}
