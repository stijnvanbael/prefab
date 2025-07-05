package be.appify.prefab.processor.kafka;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class DynamicSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final JsonSerializer<Object> jsonSerializer = new JsonSerializer<>();

    public DynamicSerializer(KafkaProperties kafkaProperties) {
        jsonSerializer.configure(kafkaProperties.buildProducerProperties(), false);
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data instanceof String string) {
            return stringSerializer.serialize(topic, string);
        }
        return jsonSerializer.serialize(topic, data);
    }
}
