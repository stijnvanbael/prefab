package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;

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
    public PrefabStream from(Class<?> type) {
        var topic = topicResolver.topicForType(type);
        var valueSerde = new SerdeAdapter<>(serializer, deserializer);
        var stream = streamsBuilder.stream(topic, Consumed.with(Serdes.String(), valueSerde));
        return new KafkaPrefabStream(stream, topicResolver, serializer, deserializer);
    }
}

