package be.appify.prefab.core.kafka;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/** A Kafka serializer that dynamically chooses between StringSerializer and JsonSerializer based on the data type. */
public class DynamicSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final JacksonJsonSerializer<Object> jsonSerializer = new JacksonJsonSerializer<>();

    /**
     * Constructs a DynamicSerializer and configures the underlying JsonSerializer with the provided Kafka properties.
     * @param kafkaProperties the Kafka properties to configure the JsonSerializer
     */
    public DynamicSerializer(KafkaProperties kafkaProperties) {
        jsonSerializer.configure(kafkaProperties.buildProducerProperties(), false);
    }

    /**
     * Serialize the given data based on its type.
     * @param topic the topic to which the data is being serialized
     * @param data the data to serialize
     * @return the serialized byte array
     */
    @Override
    public byte[] serialize(String topic, Object data) {
        if (data instanceof String string) {
            return stringSerializer.serialize(topic, string);
        }
        return jsonSerializer.serialize(topic, data);
    }
}
