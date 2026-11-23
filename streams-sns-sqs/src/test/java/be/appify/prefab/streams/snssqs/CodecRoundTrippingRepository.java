package be.appify.prefab.streams.snssqs;

import be.appify.prefab.streams.engine.InMemoryStreamStateRepository;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.postgres.JsonStreamStateCodec;
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
