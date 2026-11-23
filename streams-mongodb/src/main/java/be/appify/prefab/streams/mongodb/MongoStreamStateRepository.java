package be.appify.prefab.streams.mongodb;

import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamStateCodec;
import be.appify.prefab.streams.engine.StreamStateRepository;
import com.mongodb.MongoException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;

import static be.appify.prefab.streams.mongodb.MongoStreamSchema.INBOX;
import static be.appify.prefab.streams.mongodb.MongoStreamSchema.LEASE;
import static be.appify.prefab.streams.mongodb.MongoStreamSchema.OUTBOX;
import static be.appify.prefab.streams.mongodb.MongoStreamSchema.STORE;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * {@link StreamStateRepository} on MongoDB.
 *
 * <p>Reads happen while the engine runs a message. Changes are collected in the transaction and written in one
 * MongoDB multi-document transaction on {@link Transaction#commit()}: inbox identities, state documents guarded by a
 * version, and outbox documents. A stale version, a concurrently claimed inbox identity or a write conflict raises an
 * {@link OptimisticConflictException}, which the engine retries.
 *
 * <p>MongoDB only supports transactions on a replica set or sharded cluster, so the constructor fails with a
 * {@link MongoTransactionsUnavailableException} on a standalone server.
 */
public final class MongoStreamStateRepository implements StreamStateRepository {
    private static final int WRITE_CONFLICT = 112;
    private static final int DUPLICATE_KEY = 11000;

    private final MongoTemplate mongo;
    private final TransactionTemplate transactions;
    private final StreamStateCodec codec;

    /**
     * Creates a repository.
     *
     * @param mongo the template of the database holding the {@code prefab_stream_*} collections
     * @param codec the value and key codec
     */
    public MongoStreamStateRepository(MongoTemplate mongo, StreamStateCodec codec) {
        this.mongo = Objects.requireNonNull(mongo, "mongo must not be null");
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        MongoStreamSchema.requireTransactions(mongo);
        // Deliberately not a bean: it would become the application's primary transaction manager.
        this.transactions = new TransactionTemplate(new MongoTransactionManager(mongo.getMongoDatabaseFactory()));
    }

    @Override
    public Transaction beginTransaction() {
        return new MongoTransaction();
    }

    @Override
    public void cleanupExpired(Instant now, Duration inboxRetention) {
        if (now == null || inboxRetention == null || inboxRetention.isNegative()) {
            throw new IllegalArgumentException("now and non-negative inboxRetention are required");
        }
        mongo.remove(Query.query(where("recordedAt").lt(Date.from(now.minus(inboxRetention)))), INBOX);
        mongo.remove(Query.query(where("expiresAt").lte(Date.from(now))), LEASE);
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
        if (!claimLease(topic, owner, now, lease)) {
            return List.of();
        }
        return mongo.find(Query.query(where("topic").is(topic)).with(Sort.by("seq")).limit(limit),
                        Document.class, OUTBOX).stream()
                .map(document -> new OutboxRecord(
                        document.getString("_id"),
                        document.getString("topic"),
                        codec.decodeMessage(bytes(document.get("message")))))
                .toList();
    }

    private boolean claimLease(String topic, String owner, Instant now, Duration lease) {
        var claimable = new Criteria().andOperator(
                where("_id").is(topic),
                new Criteria().orOperator(where("owner").is(owner), where("expiresAt").lte(Date.from(now))));
        try {
            mongo.findAndModify(Query.query(claimable),
                    new Update().set("owner", owner).set("expiresAt", Date.from(now.plus(lease))),
                    FindAndModifyOptions.options().upsert(true), Document.class, LEASE);
            return true;
        } catch (DuplicateKeyException e) {
            // The upsert collided with the lease of another owner that has not expired yet.
            return false;
        }
    }

    @Override
    public void completeOutbox(String owner, String recordId) {
        var record = mongo.findOne(Query.query(where("_id").is(recordId)), Document.class, OUTBOX);
        if (record == null) {
            return;
        }
        var leased = mongo.exists(Query.query(where("_id").is(record.getString("topic"))
                .and("owner").is(owner)
                .and("expiresAt").gt(new Date())), LEASE);
        if (!leased) {
            throw new IllegalStateException(
                    "Outbox record %s is not leased by %s or the topic lease expired".formatted(recordId, owner));
        }
        mongo.remove(Query.query(where("_id").is(recordId)), OUTBOX);
    }

    private static byte[] bytes(Object stored) {
        return stored instanceof Binary binary ? binary.getData() : (byte[]) stored;
    }

    private static Document stateId(StateKey key) {
        return new Document("namespace", key.namespace()).append("key", new Binary(key.bytes()));
    }

    private static boolean isConflict(Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof DuplicateKeyException || cause instanceof ConcurrencyFailureException) {
                return true;
            }
            if (cause instanceof MongoException mongoException && (
                    mongoException.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                            || mongoException.getCode() == WRITE_CONFLICT
                            || mongoException.getCode() == DUPLICATE_KEY)) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    private final class MongoTransaction implements Transaction {
        private final Map<StateKey, Long> observedVersions = new LinkedHashMap<>();
        private final Map<StateKey, Change> changes = new LinkedHashMap<>();
        private final Set<InboxKey> pendingInbox = new LinkedHashSet<>();
        private final List<OutboxRecord> pendingOutbox = new ArrayList<>();
        private boolean complete;

        @Override
        public boolean recordInbox(String subscription, String messageId) {
            ensureOpen();
            var key = new InboxKey(subscription, messageId);
            if (pendingInbox.contains(key) || mongo.exists(Query.query(where("_id").is(key.id())), INBOX)) {
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
            var stored = mongo.findOne(Query.query(where("_id").is(stateId(stateKey))), Document.class, STORE);
            if (stored == null) {
                observedVersions.putIfAbsent(stateKey, 0L);
                return Optional.empty();
            }
            observedVersions.putIfAbsent(stateKey, stored.get("version", Number.class).longValue());
            return Optional.ofNullable(codec.decodeValue(bytes(stored.get("value"))));
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
            var query = Query.query(where("_id.namespace").is(namespace)
                    .and("expiryKey").is(new Binary(expiryBytes))
                    .and("expiresAt").lte(Date.from(streamTime)));
            mongo.find(query, Document.class, STORE).forEach(stored -> {
                var id = stored.get("_id", Document.class);
                var stateKey = new StateKey(namespace, ByteBuffer.wrap(bytes(id.get("key"))));
                observedVersions.putIfAbsent(stateKey, stored.get("version", Number.class).longValue());
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
            } catch (OptimisticConflictException e) {
                throw e;
            } catch (RuntimeException e) {
                if (isConflict(e)) {
                    throw new OptimisticConflictException(
                            "Write conflict while committing stream state: " + e.getMessage());
                }
                throw e;
            } finally {
                complete = true;
            }
        }

        private void apply() {
            var now = new Date();
            for (var inbox : pendingInbox) {
                try {
                    mongo.insert(new Document("_id", inbox.id()).append("recordedAt", now), INBOX);
                } catch (DuplicateKeyException e) {
                    throw new OptimisticConflictException("Inbox identity was concurrently claimed: " + inbox);
                }
            }
            changes.forEach(this::applyChange);
            var nextSequence = new HashMap<String, Long>();
            for (var record : pendingOutbox) {
                var sequence = nextSequence.compute(record.topic(),
                        (topic, previous) -> previous == null ? firstSequence(topic) : previous + 1);
                mongo.insert(new Document("_id", record.id())
                        .append("seq", sequence)
                        .append("topic", record.topic())
                        .append("message", new Binary(codec.encodeMessage(record.message()))), OUTBOX);
            }
        }

        // Microsecond wall clock, but always above what the topic already holds so a skewed clock cannot reorder
        // messages. Messages of one key never commit concurrently, because they conflict on the state document.
        private long firstSequence(String topic) {
            var micros = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
            var latest = mongo.findOne(Query.query(where("topic").is(topic)).with(Sort.by(Sort.Direction.DESC, "seq")),
                    Document.class, OUTBOX);
            return latest == null ? micros : Math.max(micros, latest.get("seq", Number.class).longValue() + 1);
        }

        private void applyChange(StateKey key, Change change) {
            var expected = observedVersions.getOrDefault(key, 0L);
            boolean changed;
            if (change.deleted()) {
                changed = expected == 0 || mongo.remove(versioned(key, expected), STORE).getDeletedCount() == 1;
            } else if (expected == 0) {
                changed = insertState(key, change);
            } else {
                changed = mongo.updateFirst(versioned(key, expected), updateOf(change), STORE).getMatchedCount() == 1;
            }
            if (!changed) {
                throw new OptimisticConflictException(
                        "State changed concurrently in namespace '%s'".formatted(key.namespace()));
            }
        }

        private boolean insertState(StateKey key, Change change) {
            var document = new Document("_id", stateId(key))
                    .append("value", new Binary(codec.encodeValue(change.value())))
                    .append("version", 1L);
            if (change.expiration() != null) {
                document.append("expiryKey", new Binary(change.expiration().key()))
                        .append("expiresAt", Date.from(change.expiration().expiresAt()));
            }
            try {
                mongo.insert(document, STORE);
                return true;
            } catch (DuplicateKeyException e) {
                return false;
            }
        }

        private Update updateOf(Change change) {
            var update = new Update()
                    .set("value", new Binary(codec.encodeValue(change.value())))
                    .inc("version", 1L);
            var expiration = change.expiration();
            if (expiration == null) {
                return update.unset("expiryKey").unset("expiresAt");
            }
            return update.set("expiryKey", new Binary(expiration.key()))
                    .set("expiresAt", Date.from(expiration.expiresAt()));
        }

        private Query versioned(StateKey key, long version) {
            return Query.query(where("_id").is(stateId(key)).and("version").is(version));
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
        Document id() {
            return new Document("subscription", subscription).append("messageId", messageId);
        }
    }

    private record Change(Object value, boolean deleted, Expiration expiration) {
    }

    private record Expiration(byte[] key, Instant expiresAt) {
        boolean matches(byte[] expiryKey, Instant streamTime) {
            return Arrays.equals(key, expiryKey) && !expiresAt.isAfter(streamTime);
        }
    }
}
