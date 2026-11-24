package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamStateCodec;
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
import javax.sql.DataSource;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link StreamStateRepository} on Postgres.
 *
 * <p>Reads happen while the engine runs a message. Changes are collected in the transaction and written in one
 * database transaction on {@link Transaction#commit()}: inbox identities, state rows guarded by a version, and
 * outbox rows. A stale version, a concurrently claimed inbox identity or a deadlock raises an
 * {@link OptimisticConflictException}, which the engine retries.
 *
 * <p>The transaction manager must manage the {@link DataSource} object of the {@link JdbcTemplate}. Otherwise every
 * statement would autocommit on its own, and a conflict halfway through a commit would leave the inbox identity and
 * part of the state behind while the remaining state and the outbox rows were lost. The repository therefore refuses
 * such a combination.
 */
public final class PostgresStreamStateRepository implements StreamStateRepository {
    private static final Duration CLOCK_SKEW_GUARD = Duration.ofSeconds(1);
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

    private static final String DATA_SOURCE_MISMATCH = "The transaction manager must manage the same DataSource "
            + "object as the JdbcTemplate, so a message's inbox, state and outbox changes commit atomically";

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final TransactionTemplate transactions;
    private final StreamStateCodec codec;

    /**
     * Creates a repository.
     *
     * @param jdbc the template of the database holding the {@code prefab_stream_*} tables
     * @param transactionManager the manager of the template's {@link DataSource}, used for the per-message
     *         transaction
     * @param codec the value and key codec
     * @throws IllegalArgumentException if the transaction manager manages another {@link DataSource}
     */
    public PostgresStreamStateRepository(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            StreamStateCodec codec
    ) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.dataSource = unwrap(Objects.requireNonNull(jdbc.getDataSource(), "jdbc must have a DataSource"));
        Objects.requireNonNull(transactionManager, "transactionManager must not be null");
        if (transactionManager instanceof DataSourceTransactionManager manager
                && unwrap(manager.getDataSource()) != dataSource) {
            throw new IllegalArgumentException(DATA_SOURCE_MISMATCH);
        }
        this.transactions = new TransactionTemplate(transactionManager);
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
    }

    private static DataSource unwrap(DataSource dataSource) {
        return dataSource instanceof TransactionAwareDataSourceProxy proxy ? proxy.getTargetDataSource() : dataSource;
    }

    @Override
    public Transaction beginTransaction() {
        return new PostgresTransaction();
    }

    @Override
    public void heartbeatMember(String groupId, String memberId, String topologyId, Instant now, Duration lease) {
        requireIdentity(groupId, "groupId");
        requireIdentity(memberId, "memberId");
        requireIdentity(topologyId, "topologyId");
        requirePositive(lease, "lease");
        transactions.executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO prefab_stream_member_group (group_id, topology_id) VALUES (?, ?)
                    ON CONFLICT (group_id) DO NOTHING""", groupId, topologyId);
            var registeredTopology = jdbc.queryForObject("""
                    SELECT topology_id FROM prefab_stream_member_group WHERE group_id = ? FOR UPDATE""",
                    String.class, groupId);
            if (!topologyId.equals(registeredTopology)) {
                var activeMember = Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS (
                            SELECT 1 FROM prefab_stream_member
                            WHERE group_id = ? AND member_id <> ? AND topology_id = ? AND expires_at > ?)""",
                        Boolean.class, groupId, memberId, registeredTopology,
                        Timestamp.from(now.minus(CLOCK_SKEW_GUARD))));
                var activeSchedule = Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS (
                            SELECT 1 FROM prefab_stream_schedule
                            WHERE group_id = ? AND lease_expires_at > ?)""",
                        Boolean.class, groupId, Timestamp.from(now.minus(CLOCK_SKEW_GUARD))));
                if (activeMember || activeSchedule) {
                    throw new IllegalStateException(
                            "Topology declaration changed while group members or callbacks are still active: "
                                    + groupId);
                }
                jdbc.update("UPDATE prefab_stream_member_group SET topology_id = ? WHERE group_id = ?",
                        topologyId, groupId);
            }
            jdbc.update("""
                    INSERT INTO prefab_stream_member (group_id, member_id, topology_id, expires_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (group_id, member_id) DO UPDATE
                    SET topology_id = EXCLUDED.topology_id, expires_at = EXCLUDED.expires_at""",
                    groupId, memberId, topologyId, Timestamp.from(now.plus(lease)));
        });
    }

    @Override
    public List<String> activeMembers(String groupId, Instant now) {
        return jdbc.queryForList("""
                SELECT member_id FROM prefab_stream_member
                WHERE group_id = ? AND expires_at > ? ORDER BY member_id""",
                String.class, groupId, Timestamp.from(now.minus(CLOCK_SKEW_GUARD)));
    }

    @Override
    public void removeMember(String groupId, String memberId) {
        jdbc.update("DELETE FROM prefab_stream_member WHERE group_id = ? AND member_id = ?", groupId, memberId);
    }

    @Override
    public void registerScheduledTask(String groupId, String scheduleId, Duration interval, Instant now) {
        requireIdentity(groupId, "groupId");
        requireIdentity(scheduleId, "scheduleId");
        requirePositive(interval, "interval");
        var intervalMillis = interval.toMillis();
        if (intervalMillis < 1) {
            throw new IllegalArgumentException("interval must be at least one millisecond");
        }
        jdbc.update("""
                INSERT INTO prefab_stream_schedule (schedule_id, group_id, interval_millis, next_due)
                VALUES (?, ?, ?, ?) ON CONFLICT (schedule_id) DO NOTHING""",
                scheduleId, groupId, intervalMillis, Timestamp.from(now.plusMillis(intervalMillis)));
        var existing = jdbc.queryForMap("""
                SELECT group_id, interval_millis FROM prefab_stream_schedule WHERE schedule_id = ?""", scheduleId);
        if (!groupId.equals(existing.get("group_id"))
                || intervalMillis != ((Number) existing.get("interval_millis")).longValue()) {
            throw new IllegalStateException("Scheduled task identity or interval changed: " + scheduleId);
        }
    }

    @Override
    public Optional<ScheduledTaskLease> claimScheduledTask(
            String groupId, String scheduleId, String owner, Instant now, Duration lease
    ) {
        requirePositive(lease, "lease");
        return jdbc.query("""
                UPDATE prefab_stream_schedule
                SET lease_owner = ?, lease_expires_at = ?, fencing_token = fencing_token + 1
                WHERE schedule_id = ? AND group_id = ? AND next_due <= ?
                  AND (lease_owner IS NULL OR lease_expires_at <= ?)
                RETURNING group_id, schedule_id, lease_owner, fencing_token, next_due, interval_millis""",
                (rs, row) -> new ScheduledTaskLease(
                        rs.getString("group_id"),
                        rs.getString("schedule_id"),
                        rs.getString("lease_owner"),
                        rs.getLong("fencing_token"),
                        rs.getTimestamp("next_due").toInstant(),
                        Duration.ofMillis(rs.getLong("interval_millis"))),
                owner, Timestamp.from(now.plus(lease)), scheduleId, groupId, Timestamp.from(now),
                Timestamp.from(now.minus(CLOCK_SKEW_GUARD))).stream().findFirst();
    }

    @Override
    public boolean renewScheduledTask(ScheduledTaskLease lease, Instant now, Duration duration) {
        requirePositive(duration, "duration");
        return jdbc.update("""
                UPDATE prefab_stream_schedule SET lease_expires_at = ?
                WHERE schedule_id = ? AND group_id = ? AND lease_owner = ? AND fencing_token = ?
                  AND next_due = ? AND lease_expires_at > ?""",
                Timestamp.from(now.plus(duration)), lease.scheduleId(), lease.groupId(), lease.owner(),
                lease.fencingToken(), Timestamp.from(lease.dueTime()), Timestamp.from(now.plus(CLOCK_SKEW_GUARD))) == 1;
    }

    @Override
    public void cleanupExpired(Instant now, Duration inboxRetention) {
        if (now == null || inboxRetention == null || inboxRetention.isNegative()) {
            throw new IllegalArgumentException("now and non-negative inboxRetention are required");
        }
        jdbc.update("DELETE FROM prefab_stream_inbox WHERE recorded_at < ?",
                Timestamp.from(now.minus(inboxRetention)));
        jdbc.update("DELETE FROM prefab_stream_lease WHERE expires_at <= ?", Timestamp.from(now));
        jdbc.update("DELETE FROM prefab_stream_member WHERE expires_at <= ?",
                Timestamp.from(now.minus(CLOCK_SKEW_GUARD)));
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
        private ScheduleCompletion scheduleCompletion;
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
        public void completeScheduledTask(ScheduledTaskLease lease, Instant nextDue, Instant now) {
            ensureOpen();
            if (scheduleCompletion != null) {
                throw new IllegalStateException("A transaction can complete only one scheduled occurrence");
            }
            if (!nextDue.isAfter(lease.dueTime())) {
                throw new IllegalArgumentException("nextDue must be after the completed occurrence");
            }
            scheduleCompletion = new ScheduleCompletion(lease, nextDue, now);
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
            if (!TransactionSynchronizationManager.hasResource(dataSource)) {
                throw new IllegalStateException(DATA_SOURCE_MISMATCH);
            }
            var now = Timestamp.from(Instant.now());
            for (var inbox : pendingInbox) {
                if (jdbc.update(INSERT_INBOX, inbox.subscription(), inbox.messageId(), now) == 0) {
                    throw new OptimisticConflictException("Inbox identity was concurrently claimed: " + inbox);
                }
            }
            if (scheduleCompletion != null) {
                var lease = scheduleCompletion.lease();
                var updated = jdbc.update("""
                        UPDATE prefab_stream_schedule
                        SET next_due = ?, lease_owner = NULL, lease_expires_at = NULL
                        WHERE schedule_id = ? AND group_id = ? AND lease_owner = ? AND fencing_token = ?
                          AND next_due = ? AND lease_expires_at > ?""",
                        Timestamp.from(scheduleCompletion.nextDue()), lease.scheduleId(), lease.groupId(),
                        lease.owner(), lease.fencingToken(), Timestamp.from(lease.dueTime()),
                        Timestamp.from(scheduleCompletion.now().plus(CLOCK_SKEW_GUARD)));
                if (updated != 1) {
                    throw new ScheduleLeaseLostException(lease.scheduleId());
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
            scheduleCompletion = null;
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

    private record ScheduleCompletion(ScheduledTaskLease lease, Instant nextDue, Instant now) {
    }

    private static void requireIdentity(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
