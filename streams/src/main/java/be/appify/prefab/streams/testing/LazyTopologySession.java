package be.appify.prefab.streams.testing;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.streams.GlobalStore;
import java.util.Optional;
import java.util.function.Supplier;

/** Session that starts the topology on first use, so a test can declare it before sending any record. */
final class LazyTopologySession implements TopologySession {
    private final Supplier<TopologySession> starter;
    private TopologySession session;

    LazyTopologySession(Supplier<TopologySession> starter) {
        this.starter = starter;
    }

    private synchronized TopologySession session() {
        if (session == null) {
            session = starter.get();
        }
        return session;
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(EventType<V> type) {
        return session().input(type);
    }
    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(EventType<V> type, Class<K> keyType) {
        return session().input(type, keyType);
    }
    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(EventType<V> type) {
        return session().output(type);
    }
    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(EventType<V> type, Class<K> keyType) {
        return session().output(type, keyType);
    }
    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, EventType<V> type) {
        return session().output(topic, type);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type) {
        return session().input(type);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type, Class<K> keyType) {
        return session().input(type, keyType);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type) {
        return session().output(type);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type, Class<K> keyType) {
        return session().output(type, keyType);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, Class<V> type) {
        return session().output(topic, type);
    }

    @Override
    public TopologyOutput<String, byte[]> output(String topic) {
        return session().output(topic);
    }

    @Override
    public <K, V> Optional<V> lookup(GlobalStore<K, V> store, K key) {
        return session().lookup(store, key);
    }

    @Override
    public TopologyInspection inspect() {
        return session().inspect();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return session().unwrap(type);
    }

    @Override
    public synchronized void close() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
