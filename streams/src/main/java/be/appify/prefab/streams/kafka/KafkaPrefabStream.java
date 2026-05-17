package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.PrefabStream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

/** Kafka-backed implementation for baseline sink DSL operations. */
public class KafkaPrefabStream implements PrefabStream {
    private final KStream<String, Object> stream;
    private final KafkaTopicResolver topicResolver;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;

    /** Constructs a new KafkaPrefabStream. */
    public KafkaPrefabStream(
            KStream<String, Object> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) {
        this.stream = stream;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public void to(Class<?> type) {
        to(topicResolver.topicForType(type));
    }

    @Override
    public void to(String topic) {
        var valueSerde = new SerdeAdapter<>(serializer, deserializer);
        stream.to(topic, Produced.with(Serdes.String(), valueSerde));
    }
}

