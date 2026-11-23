package be.appify.prefab.streams.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe repository for deterministic tests and local topology execution.
 */
public final class InMemoryStreamStateRepository implements StreamStateRepository {
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<StateKey, Object> values = new LinkedHashMap<>();
    private final Map<StateKey, Long> versions = new LinkedHashMap<>();
    private final Map<StateKey, Expiration> expirations = new LinkedHashMap<>();
    private final Map<InboxKey, Instant> inbox = new LinkedHashMap<>();
    private final Map<String, LeasedOutbox> outbox = new LinkedHashMap<>();
    private final Map<String, RelayLease> relayLeases = new LinkedHashMap<>();

    @Override
    public Transaction beginTransaction() {
        return new MemoryTransaction();
    }

    @Override
    public void cleanupExpired(Instant now, Duration inboxRetention) {
        if (now == null || inboxRetention == null || inboxRetention.isNegative()) {
            throw new IllegalArgumentException("now and non-negative inboxRetention are required");
        }
        var inboxCutoff = now.minus(inboxRetention);
        lock.lock();
        try {
            inbox.entrySet().removeIf(entry -> entry.getValue().isBefore(inboxCutoff));
            outbox.replaceAll((id, leased) -> leased.expiresAt.isAfter(now)
                    ? leased : new LeasedOutbox(null, Instant.MIN, leased.record));
        } finally {
            lock.unlock();
        }
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
        lock.lock();
        try {
            var currentLease = relayLeases.get(topic);
            if (currentLease != null && !owner.equals(currentLease.owner) && currentLease.expiresAt.isAfter(now)) {
                return List.of();
            }
            relayLeases.put(topic, new RelayLease(owner, now.plus(lease)));
            return outbox.entrySet().stream()
                    .filter(entry -> entry.getValue().record.topic().equals(topic))
                    .filter(entry -> entry.getValue().owner == null || owner.equals(entry.getValue().owner)
                            || !entry.getValue().expiresAt.isAfter(now))
                    .limit(limit)
                    .peek(entry -> entry.setValue(new LeasedOutbox(owner, now.plus(lease), entry.getValue().record)))
                    .map(Map.Entry::getValue)
                    .map(LeasedOutbox::record)
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void completeOutbox(String owner, String recordId) {
        lock.lock();
        try {
            var leased = outbox.get(recordId);
            if (leased == null) {
                return;
            }
            if (!owner.equals(leased.owner)) {
                throw new IllegalStateException("Outbox record is leased by a different owner: " + recordId);
            }
            var topicLease = relayLeases.get(leased.record.topic());
            if (topicLease == null || !owner.equals(topicLease.owner) || !topicLease.expiresAt.isAfter(Instant.now())) {
                throw new IllegalStateException("Outbox topic lease expired before completing record: " + recordId);
            }
            outbox.remove(recordId);
        } finally {
            lock.unlock();
        }
    }

    /** Returns the current value for test assertions. */
    public Optional<Object> read(String namespace, Object key) {
        lock.lock();
        try {
            return Optional.ofNullable(values.get(new StateKey(namespace, key)));
        } finally {
            lock.unlock();
        }
    }

    private final class MemoryTransaction implements Transaction {
        private final Map<StateKey, Long> observedVersions = new LinkedHashMap<>();
        private final Map<StateKey, Change> changes = new LinkedHashMap<>();
        private final Set<InboxKey> pendingInbox = new LinkedHashSet<>();
        private final List<OutboxRecord> pendingOutbox = new ArrayList<>();
        private boolean complete;

        @Override
        public boolean recordInbox(String subscription, String messageId) {
            ensureOpen();
            var key = new InboxKey(subscription, messageId);
            lock.lock();
            try {
                if (inbox.containsKey(key) || pendingInbox.contains(key)) {
                    return false;
                }
            } finally {
                lock.unlock();
            }
            pendingInbox.add(key);
            return true;
        }

        @Override
        public Optional<Object> get(String namespace, Object key) {
            ensureOpen();
            var stateKey = new StateKey(namespace, key);
            var change = changes.get(stateKey);
            if (change != null) {
                return change.deleted ? Optional.empty() : Optional.ofNullable(change.value);
            }
            lock.lock();
            try {
                observedVersions.putIfAbsent(stateKey, versions.getOrDefault(stateKey, 0L));
                return Optional.ofNullable(values.get(stateKey));
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void put(String namespace, Object key, Object value) {
            put(namespace, key, value, null);
        }

        @Override
        public void putExpiring(
                String namespace,
                Object key,
                Object expiryKey,
                Object value,
                Instant expiresAt
        ) {
            if (expiresAt == null) {
                throw new IllegalArgumentException("expiresAt must not be null");
            }
            put(namespace, key, value, new Expiration(expiryKey, expiresAt));
        }

        private void put(String namespace, Object key, Object value, Expiration expiration) {
            ensureOpen();
            var stateKey = new StateKey(namespace, key);
            observeVersion(stateKey);
            changes.put(stateKey, new Change(value, false, expiration));
        }

        @Override
        public void deleteExpired(String namespace, Object expiryKey, Instant streamTime) {
            ensureOpen();
            if (streamTime == null) {
                throw new IllegalArgumentException("streamTime must not be null");
            }
            lock.lock();
            try {
                var expired = expirations.entrySet().stream()
                        .filter(entry -> entry.getKey().namespace.equals(namespace))
                        .filter(entry -> Objects.equals(entry.getValue().key, expiryKey))
                        .filter(entry -> !entry.getValue().expiresAt.isAfter(streamTime))
                        .map(Map.Entry::getKey)
                        .toList();
                expired.forEach(stateKey -> delete(stateKey.namespace, stateKey.key));
            } finally {
                lock.unlock();
            }
            changes.entrySet().stream()
                    .filter(entry -> entry.getKey().namespace.equals(namespace))
                    .filter(entry -> !entry.getValue().deleted)
                    .filter(entry -> entry.getValue().expiration != null)
                    .filter(entry -> Objects.equals(entry.getValue().expiration.key, expiryKey))
                    .filter(entry -> !entry.getValue().expiration.expiresAt.isAfter(streamTime))
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(stateKey -> delete(stateKey.namespace, stateKey.key));
        }

        @Override
        public void delete(String namespace, Object key) {
            ensureOpen();
            var stateKey = new StateKey(namespace, key);
            observeVersion(stateKey);
            changes.put(stateKey, new Change(null, true, null));
        }

        @Override
        public void enqueue(String topic, StreamMessage message) {
            ensureOpen();
            pendingOutbox.add(new OutboxRecord(UUID.randomUUID().toString(), topic, message));
        }

        @Override
        public void commit() {
            ensureOpen();
            lock.lock();
            try {
                if (pendingInbox.stream().anyMatch(inbox::containsKey)) {
                    throw new OptimisticConflictException("Inbox identity was concurrently claimed");
                }
                for (var entry : changes.entrySet()) {
                    var actual = versions.getOrDefault(entry.getKey(), 0L);
                    var expected = observedVersions.getOrDefault(entry.getKey(), actual);
                    if (actual != expected) {
                        throw new OptimisticConflictException(
                                "State changed concurrently in namespace '%s' for key %s"
                                        .formatted(entry.getKey().namespace, entry.getKey().key));
                    }
                }
                pendingInbox.forEach(key -> inbox.put(key, Instant.now()));
                changes.forEach((key, change) -> {
                    var nextVersion = versions.getOrDefault(key, 0L) + 1;
                    versions.put(key, nextVersion);
                    if (change.deleted) {
                        values.remove(key);
                        expirations.remove(key);
                    } else {
                        values.put(key, change.value);
                        if (change.expiration == null) {
                            expirations.remove(key);
                        } else {
                            expirations.put(key, change.expiration);
                        }
                    }
                });
                pendingOutbox.forEach(record -> outbox.put(
                        record.id(),
                        new LeasedOutbox(null, Instant.MIN, record)));
                complete = true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void rollback() {
            complete = true;
            changes.clear();
            pendingInbox.clear();
            pendingOutbox.clear();
        }

        private void observeVersion(StateKey key) {
            if (!observedVersions.containsKey(key)) {
                get(key.namespace, key.key);
            }
        }

        private void ensureOpen() {
            if (complete) {
                throw new IllegalStateException("Transaction is already complete");
            }
        }
    }

    private record StateKey(String namespace, Object key) {
    }

    private record InboxKey(String subscription, String messageId) {
    }

    private record Change(Object value, boolean deleted, Expiration expiration) {
    }

    private record Expiration(Object key, Instant expiresAt) {
    }

    private record LeasedOutbox(String owner, Instant expiresAt, OutboxRecord record) {
    }

    private record RelayLease(String owner, Instant expiresAt) {
    }
}
