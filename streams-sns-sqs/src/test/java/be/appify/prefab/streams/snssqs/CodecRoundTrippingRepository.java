package be.appify.prefab.streams.snssqs;

import be.appify.prefab.streams.engine.InMemoryStreamStateRepository;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * An in-memory repository that stores state and outbox messages as the Postgres repository does: encoded and decoded
 * by the {@link JsonStreamStateCodec}. It finds codecs that lose the type of nested values without a database.
 */
final class CodecRoundTrippingRepository implements StreamStateRepository {
    private final InMemoryStreamStateRepository delegate = new InMemoryStreamStateRepository();
    private final JsonStreamStateCodec codec = new JsonStreamStateCodec();

    @Override
    public Transaction beginTransaction() {
        return new RoundTrippingTransaction(delegate.beginTransaction());
    }

    @Override
    public void heartbeatMember(String groupId, String memberId, String topologyId, Instant now, Duration lease) {
        delegate.heartbeatMember(groupId, memberId, topologyId, now, lease);
    }

    @Override
    public List<String> activeMembers(String groupId, Instant now) {
        return delegate.activeMembers(groupId, now);
    }

    @Override
    public void removeMember(String groupId, String memberId) {
        delegate.removeMember(groupId, memberId);
    }

    @Override
    public void registerScheduledTask(String groupId, String scheduleId, Duration interval, Instant now) {
        delegate.registerScheduledTask(groupId, scheduleId, interval, now);
    }

    @Override
    public Optional<ScheduledTaskLease> claimScheduledTask(
            String groupId, String scheduleId, String owner, Instant now, Duration lease
    ) {
        return delegate.claimScheduledTask(groupId, scheduleId, owner, now, lease);
    }

    @Override
    public boolean renewScheduledTask(ScheduledTaskLease lease, Instant now, Duration duration) {
        return delegate.renewScheduledTask(lease, now, duration);
    }

    @Override
    public void cleanupExpired(Instant now, Duration inboxRetention) {
        delegate.cleanupExpired(now, inboxRetention);
    }

    @Override
    public List<OutboxRecord> leaseOutbox(String topic, String owner, Instant now, Duration lease, int limit) {
        return delegate.leaseOutbox(topic, owner, now, lease, limit);
    }

    @Override
    public void completeOutbox(String owner, String recordId) {
        delegate.completeOutbox(owner, recordId);
    }

    private final class RoundTrippingTransaction implements Transaction {
        private final Transaction inner;

        private RoundTrippingTransaction(Transaction inner) {
            this.inner = inner;
        }

        @Override
        public boolean recordInbox(String subscription, String messageId) {
            return inner.recordInbox(subscription, messageId);
        }

        @Override
        public Optional<Object> get(String namespace, Object key) {
            return inner.get(namespace, key);
        }

        @Override
        public void put(String namespace, Object key, Object value) {
            inner.put(namespace, key, roundTrip(value));
        }

        @Override
        public void putExpiring(String namespace, Object key, Object expiryKey, Object value, Instant expiresAt) {
            inner.putExpiring(namespace, key, expiryKey, roundTrip(value), expiresAt);
        }

        @Override
        public void deleteExpired(String namespace, Object expiryKey, Instant streamTime) {
            inner.deleteExpired(namespace, expiryKey, streamTime);
        }

        @Override
        public void delete(String namespace, Object key) {
            inner.delete(namespace, key);
        }

        @Override
        public void enqueue(String topic, StreamMessage message) {
            inner.enqueue(topic, codec.decodeMessage(codec.encodeMessage(message)));
        }

        @Override
        public void completeScheduledTask(ScheduledTaskLease lease, Instant nextDue, Instant now) {
            inner.completeScheduledTask(lease, nextDue, now);
        }

        @Override
        public void commit() {
            inner.commit();
        }

        @Override
        public void rollback() {
            inner.rollback();
        }

        private Object roundTrip(Object value) {
            return codec.decodeValue(codec.encodeValue(value));
        }
    }
}
