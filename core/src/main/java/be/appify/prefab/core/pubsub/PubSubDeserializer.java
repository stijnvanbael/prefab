package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.util.SerializationRegistry;
import com.google.protobuf.ByteString;
import java.io.IOException;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 * A deserializer for Pub/Sub messages that dynamically chooses the deserialization method based on the topic's serialization format. It uses a
 * {@link SerializationRegistry} to determine the serialization format for each topic and delegates to the appropriate deserialization method.
 */
@Component
public class PubSubDeserializer {
    private final JsonUtil jsonUtil;
    private final SerializationRegistry serializationRegistry;
    private final ConversionService conversionService;

    /**
     * Constructs a PubSubDeserializer with the given JsonUtil, SerializationRegistry, and ConversionService.
     *
     * @param jsonUtil
     *         the JsonUtil to use for JSON deserialization
     * @param serializationRegistry
     *         the SerializationRegistry that contains the serialization format for each topic
     * @param conversionService
     *         the ConversionService to convert GenericRecord to the target event class for Avro deserialization
     */
    public PubSubDeserializer(JsonUtil jsonUtil, SerializationRegistry serializationRegistry, ConversionService conversionService) {
        this.jsonUtil = jsonUtil;
        this.serializationRegistry = serializationRegistry;
        this.conversionService = conversionService;
    }

    /**
     * Deserialize the given data based on the topic's serialization format.
     *
     * @param topic
     *         the topic from which the data is being deserialized
     * @param data
     *         the data to deserialize
     * @param type
     *         the class of the object to return
     * @param <T>
     *         the type of the object to return
     * @return the deserialized object
     */
    public <T> T deserialize(String topic, ByteString data, Class<T> type) {
        var serialization = serializationRegistry.get(topic);
        return switch (serialization) {
            case AVRO -> deserializeAvro(data.toByteArray(), type);
            case JSON -> jsonUtil.parseJson(data.toStringUtf8(), type);
        };
    }

    private <T> T deserializeAvro(byte[] data, Class<T> type) {
        var avroReader = new GenericDatumReader<GenericRecord>();
        var decoder = new DecoderFactory().binaryDecoder(data, null);
        GenericRecord genericRecord;
        try {
            genericRecord = avroReader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize data from Avro", e);
        }
        return conversionService.convert(genericRecord, type);
    }
}
