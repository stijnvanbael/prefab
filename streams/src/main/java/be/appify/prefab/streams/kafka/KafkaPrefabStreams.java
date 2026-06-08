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
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
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
        KStream<K, V> stream = streamsBuilder.stream(topic,
                Consumed.with(new StringKeySerde<>(keyType), valueSerde));
        return new KafkaPrefabStream<>(streamsBuilder, stream, topicResolver, serializer, deserializer, type, this, keyType);
    }

    @SuppressWarnings("unchecked")
    static <K extends Key<K>, V extends Keyed<K>> Class<K> keyTypeOf(Class<V> valueType) {
        return (Class<K>) resolveKeyType(valueType, Map.of(), valueType);
    }

    private static Class<?> resolveKeyType(Class<?> type, Map<TypeVariable<?>, Type> subs, Class<?> originalType) {
        // Check direct generic interfaces for Keyed<...>
        for (var genericInterface : type.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt && pt.getRawType() == Keyed.class) {
                return extractConcreteClass(resolve(pt.getActualTypeArguments()[0], subs), originalType);
            }
        }
        // Recurse into each super-interface
        for (var genericInterface : type.getGenericInterfaces()) {
            var rawInterface = toRawClass(genericInterface);
            if (rawInterface == null || rawInterface == Keyed.class) {
                continue;
            }
            try {
                return resolveKeyType(rawInterface, substitutionsFor(rawInterface, genericInterface, subs), originalType);
            } catch (IllegalArgumentException ignored) {
                // not found in this interface branch; continue searching
            }
        }
        // Recurse into superclass
        var superClass = type.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return resolveKeyType(superClass, substitutionsFor(superClass, type.getGenericSuperclass(), subs), originalType);
        }
        throw new IllegalArgumentException("Cannot resolve key type for %s".formatted(originalType.getName()));
    }

    private static Type resolve(Type type, Map<TypeVariable<?>, Type> subs) {
        return type instanceof TypeVariable<?> tv ? subs.getOrDefault(tv, tv) : type;
    }

    private static Class<?> extractConcreteClass(Type type, Class<?> originalType) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            return c;
        }
        throw new IllegalArgumentException("Cannot resolve key type for %s".formatted(originalType.getName()));
    }

    private static Class<?> toRawClass(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            return c;
        }
        return null;
    }

    private static Map<TypeVariable<?>, Type> substitutionsFor(Class<?> rawClass, Type genericType,
            Map<TypeVariable<?>, Type> parentSubs) {
        if (!(genericType instanceof ParameterizedType pt)) {
            return parentSubs;
        }
        var params = rawClass.getTypeParameters();
        var args = pt.getActualTypeArguments();
        if (params.length == 0) {
            return parentSubs;
        }
        var result = new HashMap<>(parentSubs);
        for (var i = 0; i < params.length; i++) {
            result.put(params[i], resolve(args[i], parentSubs));
        }
        return Map.copyOf(result);
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
