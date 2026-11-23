package be.appify.prefab.streams.engine;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.testing.TopologyInput;
import be.appify.prefab.streams.testing.TopologyInspection;
import be.appify.prefab.streams.testing.TopologyOutput;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.tracing.MessageTracing;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.ResolvableType;
import tools.jackson.databind.json.JsonMapper;

/** Running test session backed by the in-memory engine. */
public final class InMemoryTopologySession implements TopologySession {
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();
    private final String appId;
    private final InMemoryTopologyTestHarness.InMemoryTopicResolver topicResolver;
    private final InMemoryStreamSource source;
    private final InMemoryStreamSink sink;
    private final InMemoryStreamStateRepository repository;
    private final InProcessPrefabStreams.RunningTopology topology;
    private final AtomicBoolean closed = new AtomicBoolean();

    InMemoryTopologySession(
            String appId,
            InMemoryTopologyTestHarness.InMemoryTopicResolver topicResolver,
            InMemoryStreamSource source,
            InMemoryStreamSink sink,
            InMemoryStreamStateRepository repository,
            InProcessPrefabStreams.RunningTopology topology
    ) {
        this.appId = appId;
        this.topicResolver = topicResolver;
        this.source = source;
        this.sink = sink;
        this.repository = repository;
        this.topology = topology;
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(EventType<V> type) {
        return input(type, keyType(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(EventType<V> type, Class<K> keyType) {
        return new Input<>(topicResolver.topicFor(type), type.id());
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(EventType<V> type) {
        return output(type, keyType(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(EventType<V> type, Class<K> keyType) {
        return new Output<>(topicResolver.topicFor(type), type.rawType(), type.id());
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, EventType<V> type) {
        return new Output<>(topic, type.rawType(), type.id());
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type) {
        return input(type, keyType(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type, Class<K> keyType) {
        return input(topicResolver.topicFor(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type) {
        return output(type, keyType(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type, Class<K> keyType) {
        return output(topicResolver.topicFor(type), type);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, Class<V> type) {
        return new Output<>(topic, type);
    }

    @Override
    public TopologyOutput<String, byte[]> output(String topic) {
        return new Output<>(topic, byte[].class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Optional<V> lookup(GlobalStore<K, V> store, K key) {
        return repository.read(store.name(), key).map(value -> (V) value);
    }

    @Override
    public TopologyInspection inspect() {
        return topology.inspect();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(source)) {
            return type.cast(source);
        }
        if (type.isInstance(sink)) {
            return type.cast(sink);
        }
        throw new IllegalArgumentException("In-memory topology sessions cannot be unwrapped to " + type.getName());
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            topology.close();
        }
    }

    private <K, V extends Keyed<K>> TopologyInput<K, V> input(String topic) {
        return new Input<>(topic, null);
    }

    @SuppressWarnings("unchecked")
    private static <K, V extends Keyed<K>> Class<K> keyType(Class<V> type) {
        var keyType = ResolvableType.forClass(type).as(Keyed.class).getGeneric(0).resolve();
        if (keyType == null) {
            throw new IllegalArgumentException("Cannot resolve key type for " + type.getName());
        }
        return (Class<K>) keyType;
    }

    @SuppressWarnings("unchecked")
    private static <K, V extends Keyed<K>> Class<K> keyType(EventType<V> type) {
        var keyType = ResolvableType.forType(type.type()).as(Keyed.class).getGeneric(0).resolve();
        if (keyType == null) {
            throw new IllegalArgumentException("Cannot resolve key type for " + type);
        }
        return (Class<K>) keyType;
    }

    private final class Input<K, V extends Keyed<K>> implements TopologyInput<K, V> {
        private final String topic;
        private final String eventTypeId;

        private Input(String topic, String eventTypeId) {
            this.topic = topic;
            this.eventTypeId = eventTypeId;
        }

        @Override
        public TopologyInput<K, V> send(K key, V value) {
            return send(key, value, Instant.now());
        }

        @Override
        public TopologyInput<K, V> send(K key, V value, Instant timestamp) {
            deliver(key, value, timestamp, UUID.randomUUID().toString(), Map.of());
            return this;
        }

        @Override
        public TopologyInput<K, V> sendTombstone(K key) {
            deliver(key, null, Instant.now(), UUID.randomUUID().toString(), Map.of());
            return this;
        }

        @Override
        public TopologyInput<K, V> sendWithMessageId(V value, String messageId) {
            return sendWithMessageId(value, messageId, Instant.now());
        }

        @Override
        public TopologyInput<K, V> sendWithMessageId(V value, String messageId, Instant timestamp) {
            deliver(value.key(), value, timestamp, messageId, Map.of(MessageTracing.MESSAGE_ID_HEADER, messageId));
            return this;
        }

        private void deliver(K key, V value, Instant timestamp, String messageId, Map<String, String> headers) {
            var messageHeaders = new java.util.LinkedHashMap<>(headers);
            if (eventTypeId != null) {
                messageHeaders.put(EventType.HEADER, eventTypeId);
            }
            source.deliver(topic, new StreamMessage(messageId, key, value, messageHeaders, timestamp));
        }
    }

    private final class Output<K, V> implements TopologyOutput<K, V> {
        private final String topic;
        private final Class<?> valueType;
        private final String eventTypeId;

        private Output(String topic, Class<?> valueType) {
            this(topic, valueType, null);
        }

        private Output(String topic, Class<?> valueType, String eventTypeId) {
            this.topic = topic;
            this.valueType = valueType;
            this.eventTypeId = eventTypeId;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<V> values() {
            return drain().stream().map(message -> (V) message.value()).toList();
        }

        @Override
        public V value() {
            var values = values();
            if (values.isEmpty()) {
                throw new java.util.NoSuchElementException("No messages available for topic " + topic);
            }
            return values.getFirst();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Record<K, V>> records() {
            return drain().stream().map(message -> new Record<>((K) message.key(), (V) message.value())).toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Message<K, V>> messages() {
            return drain().stream().map(message -> new Message<>(
                    (K) message.key(),
                    (V) message.value(),
                    Optional.ofNullable(message.headers().get(MessageTracing.MESSAGE_ID_HEADER)),
                    MessageTracing.decode(message.headers().get(MessageTracing.CORRELATION_IDS_HEADER))))
                    .toList();
        }

        @Override
        public boolean isEmpty() {
            return eventTypeId == null ? sink.isEmpty(topic) : sink.isEmpty(topic, eventTypeId);
        }

        private List<StreamMessage> drain() {
            var records = sink.drain(topic, eventTypeId);
            if (valueType == byte[].class) {
                return records.stream().map(message -> new StreamMessage(
                        message.messageId(),
                        message.key(),
                        serialize(message.value()),
                        message.headers(),
                        message.eventTime())).toList();
            }
            return List.copyOf(records);
        }

        private byte[] serialize(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            try {
                return JSON.writeValueAsBytes(value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize test output from topic " + topic, e);
            }
        }
    }
}
