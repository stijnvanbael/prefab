package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.Stores;

/** Kafka-backed implementation for the baseline source DSL operation. */
public class KafkaPrefabStreams implements PrefabStreams {
    private final StreamsBuilder streamsBuilder;
    private final KafkaTopicResolver topicResolver;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;

    /**
     * Constructs a new KafkaPrefabStreams.
     */
    public KafkaPrefabStreams(
            StreamsBuilder streamsBuilder,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) {
        this.streamsBuilder = streamsBuilder;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public <K extends Key<K>, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type) {
        var topic = topicResolver.topicForType(type);
        var keyType = keyTypeOf(type);
        var valueSerde = new SerdeAdapter<V>(serializer.adapt(), deserializer.adapt());
        // DynamicSerializer/Deserializer operate on Object at runtime; the cast is safe because
        // the topic is registered for exactly this type and the serde will deserialize to V.
        KStream<K, V> stream = (KStream<K, V>) streamsBuilder.stream(topic,
                Consumed.with(new StringKeySerde<>(keyType), valueSerde));
        return new KafkaPrefabStream<>(streamsBuilder, stream, topicResolver, serializer, deserializer, type, this, keyType);
    }

    @SuppressWarnings("unchecked")
    static <K extends Key<K>, V extends Keyed<K>> Class<K> keyTypeOf(Class<V> valueType) {
        return (Class<K>) keyTypeOf(valueType, valueType);
    }

    private static Class<?> keyTypeOf(Class<?> currentType, Class<?> originalType) {
        for (Type genericInterface : currentType.getGenericInterfaces()) {
            if (!(genericInterface instanceof ParameterizedType parameterizedType)) {
                continue;
            }
            if (parameterizedType.getRawType() != Keyed.class) {
                continue;
            }
            var keyType = parameterizedType.getActualTypeArguments()[0];
            if (keyType instanceof Class<?> keyClass) {
                return keyClass;
            }
            if (keyType instanceof ParameterizedType keyParameterizedType && keyParameterizedType.getRawType() instanceof Class<?> keyClass) {
                return keyClass;
            }
        }

        var superClass = currentType.getSuperclass();
        if (superClass == null || superClass == Object.class) {
            throw new IllegalArgumentException("Cannot resolve key type for %s".formatted(originalType.getName()));
        }
        return keyTypeOf(superClass, originalType);
    }

    @Override
    public <K extends Key<K>, M extends Keyed<K>> PrefabStream<K, M> merge(PrefabStream<K, ? extends M> left,
            PrefabStream<K, ? extends M> right) {
        return KafkaPrefabStream.mergeStreams(left, right);
    }

    @Override
    public <KS extends Key<KS>, VS extends Keyed<KS>> Store<KS, VS> createStore(Class<VS> type) {
        var name = toKebabCase(type.getSimpleName());
        var keyType = keyTypeOf(type);
        streamsBuilder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(name),
                new StringKeySerde<>(keyType),
                new SerdeAdapter<>(serializer, deserializer)));
        return new KafkaPrefabStoreAdapter<>(name);
    }

    static String toKebabCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }
}
