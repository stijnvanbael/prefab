package be.appify.prefab.core.serialization;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.spring.JsonUtil;
import com.google.protobuf.ByteString;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericAvroTransportTest {
    @Event(topic = "string-avro", serialization = Event.Serialization.AVRO, typeArguments = String.class)
    @Event(topic = "integer-avro", serialization = Event.Serialization.AVRO, typeArguments = Integer.class)
    public record Changed<T>(T payload) {}

    @Test
    void transportsUseConcreteSchemaAndConverterWithoutErasedSpringLookup() {
        var strings = new EventType<Changed<String>>() {};
        var integers = new EventType<Changed<Integer>>() {};
        var registry = new EventRegistry();
        registry.register("string-avro", strings, Event.Serialization.AVRO);
        registry.register("integer-avro", integers, Event.Serialization.AVRO);
        var schema = new Schema.Parser().parse("""
                {"type":"record","name":"ChangedStrings","fields":[{"name":"payload","type":"string"}]}
                """);
        registry.registerReaderSchema(strings, schema);
        registry.registerAvroConverters(strings, event -> {
            var record = new GenericData.Record(schema);
            record.put("payload", event.payload());
            return record;
        }, record -> new Changed<>(record.get("payload").toString()));
        var event = new TypedEvent<>(strings, new Changed<>("value"));
        var properties = new KafkaProperties();
        KafkaSchemaRegistrySupport.enableMockSchemaRegistry(properties);
        var kafka = new DynamicSerializer(properties, null, registry);
        var headers = new RecordHeaders();
        var bytes = kafka.serialize("string-avro", headers, event);
        assertThat(new DynamicDeserializer(properties, null, registry)
                .deserialize("string-avro", headers, bytes)).isEqualTo(event.value());
        assertThatThrownBy(() -> kafka.serialize("integer-avro", new RecordHeaders(), event))
                .hasMessageContaining(strings.id());

        var json = new JsonUtil(JsonMapper.builder().build());
        var pubsubBytes = new PubSubSerializer(json, registry, null).serialize("string-avro", event);
        var attributes = Map.of(EventType.HEADER, strings.id());
        assertThat(new PubSubDeserializer(json, registry, null).deserialize(
                "string-avro", ByteString.copyFrom(pubsubBytes), strings, attributes)).isEqualTo(event.value());
        var sns = new SnsSerializer(json, registry, null).serialize("string-avro", event);
        assertThat(java.util.Base64.getDecoder().decode(sns)).isEqualTo(pubsubBytes);
        assertThatThrownBy(() -> registry.toAvro(new TypedEvent<>(integers, new Changed<>(1))))
                .hasMessageContaining(integers.id());
    }
}
