package be.appify.prefab.streams.kafka.testing;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.streams.kafka.ConcreteEventSerde;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.kafka.StringKeySerde;
import be.appify.prefab.streams.testing.TopologyInput;
import be.appify.prefab.streams.testing.TopologyInspection;
import be.appify.prefab.streams.testing.TopologyOutput;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.tracing.MessageTracing;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.apache.kafka.streams.state.KeyValueStore;

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.keyTypeOf;

/** {@link TopologySession} backed by a Kafka Streams {@link TopologyTestDriver}. */
final class KafkaTopologySession implements TopologySession {
    private final Topology topology;
    private final TopologyTestDriver driver;
    private final EventRegistry eventRegistry;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;

    KafkaTopologySession(
            Topology topology,
            TopologyTestDriver driver,
            EventRegistry eventRegistry,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) {
        this.topology = topology;
        this.driver = driver;
        this.eventRegistry = eventRegistry;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(EventType<V> type) {
        return input(type, keyTypeOf(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(EventType<V> type, Class<K> keyType) {
        return new KafkaInput<>(driver.createInputTopic(eventRegistry.topicForType(type),
                new StringKeySerde<>(keyType).serializer(),
                new ConcreteEventSerde<>(type, serializer, deserializer).serializer()));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(EventType<V> type) {
        return output(type, keyTypeOf(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(EventType<V> type, Class<K> keyType) {
        return new KafkaOutput<>(driver.createOutputTopic(eventRegistry.topicForType(type),
                new StringKeySerde<>(keyType).deserializer(),
                new ConcreteEventSerde<>(type, serializer, deserializer).deserializer()));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, EventType<V> type) {
        return new KafkaOutput<>(driver.createOutputTopic(topic,
                new StringKeySerde<K>(keyTypeOf(type)).deserializer(),
                new ConcreteEventSerde<>(type, serializer, deserializer).deserializer()));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type) {
        return input(type, keyTypeOf(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type, Class<K> keyType) {
        var topic = driver.createInputTopic(
                eventRegistry.topicForType(type), new StringKeySerde<>(keyType).serializer(), serializer.<V>adapt());
        return new KafkaInput<>(topic);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type) {
        return output(type, keyTypeOf(type));
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type, Class<K> keyType) {
        return output(eventRegistry.topicForType(type), type, keyType);
    }

    @Override
    public <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, Class<V> type) {
        return output(topic, type, keyTypeOf(type));
    }

    private <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, Class<V> type, Class<K> keyType) {
        return new KafkaOutput<>(driver.createOutputTopic(
                topic, new StringKeySerde<>(keyType).deserializer(), deserializer.<V>adapt()));
    }

    @Override
    public TopologyOutput<String, byte[]> output(String topic) {
        return new KafkaOutput<>(driver.createOutputTopic(topic, new StringDeserializer(), new ByteArrayDeserializer()));
    }

    @Override
    public <K, V> Optional<V> lookup(GlobalStore<K, V> store, K key) {
        KeyValueStore<K, V> keyValueStore = driver.getKeyValueStore(store.name());
        return Optional.ofNullable(keyValueStore.get(key));
    }

    @Override
    public TopologyInspection inspect() {
        var description = topology.describe();
        var storeNames = Stream.concat(
                        description.subtopologies().stream().flatMap(subtopology -> subtopology.nodes().stream())
                                .flatMap(KafkaTopologySession::storesOf),
                        description.globalStores().stream().flatMap(global -> storesOf(global.processor())))
                .distinct()
                .sorted()
                .toList();
        return new TopologyInspection(
                description.toString(),
                storeNames,
                description.globalStores().size(),
                description.subtopologies().size());
    }

    private static Stream<String> storesOf(TopologyDescription.Node node) {
        return node instanceof TopologyDescription.Processor processor ? processor.stores().stream() : Stream.empty();
    }
    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(driver)) {
            return type.cast(driver);
        }
        throw new IllegalArgumentException("Kafka topology sessions cannot be unwrapped to " + type.getName());
    }

    @Override
    public void close() {
        driver.close();
    }

    private record KafkaInput<K, V extends Keyed<K>>(TestInputTopic<K, V> topic) implements TopologyInput<K, V> {
        @Override
        public TopologyInput<K, V> send(K key, V value) {
            topic.pipeInput(key, value);
            return this;
        }

        @Override
        public TopologyInput<K, V> send(K key, V value, Instant timestamp) {
            topic.pipeInput(key, value, timestamp);
            return this;
        }

        @Override
        public TopologyInput<K, V> sendTombstone(K key) {
            topic.pipeInput(key, null);
            return this;
        }
        @Override
        public TopologyInput<K, V> sendWithMessageId(V value, String messageId) {
            return sendWithMessageId(value, messageId, Instant.now());
        }

        @Override
        public TopologyInput<K, V> sendWithMessageId(V value, String messageId, Instant timestamp) {
            var headers = new RecordHeaders()
                    .add(MessageTracing.MESSAGE_ID_HEADER, messageId.getBytes(StandardCharsets.UTF_8));
            topic.pipeInput(new TestRecord<>(value.key(), value, headers, timestamp));
            return this;
        }
    }

    private record KafkaOutput<K, V>(TestOutputTopic<K, V> topic) implements TopologyOutput<K, V> {
        @Override
        public List<Message<K, V>> messages() {
            return topic.readRecordsToList().stream()
                    .map(record -> new Message<>(
                            record.key(),
                            record.value(),
                            Optional.ofNullable(headerValue(record, MessageTracing.MESSAGE_ID_HEADER)),
                            MessageTracing.decode(headerValue(record, MessageTracing.CORRELATION_IDS_HEADER))))
                    .toList();
        }

        private static String headerValue(TestRecord<?, ?> record, String name) {
            var header = record.headers().lastHeader(name);
            return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
        }

        @Override
        public List<V> values() {
            return topic.readValuesToList();
        }

        @Override
        public V value() {
            return topic.readValue();
        }

        @Override
        public List<Record<K, V>> records() {
            return topic.readKeyValuesToList().stream()
                    .map(keyValue -> new Record<>(keyValue.key, keyValue.value))
                    .toList();
        }

        @Override
        public boolean isEmpty() {
            return topic.isEmpty();
        }
    }
}
