package be.appify.prefab.core.sns;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.spring.JsonUtil;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.EncoderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 * A serializer for SNS messages that dynamically chooses the serialization method based on the topic's serialization
 * format. It uses an {@link EventRegistry} to determine the serialization format for each topic and delegates to
 * the appropriate serialization method. If the topic is not registered, JSON serialization is used by default.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class SnsSerializer {
    private final JsonUtil jsonUtil;
    private final EventRegistry eventRegistry;
    private final ConversionService conversionService;

    /**
     * Constructs a SnsSerializer with the given JsonUtil, EventRegistry, and ConversionService.
     *
     * @param jsonUtil       the JsonUtil to use for JSON serialization
     * @param eventRegistry  the EventRegistry that contains the serialization format for each topic
     * @param conversionService the ConversionService to convert objects to GenericRecord for Avro serialization
     */
    public SnsSerializer(JsonUtil jsonUtil, EventRegistry eventRegistry, ConversionService conversionService) {
        this.jsonUtil = jsonUtil;
        this.eventRegistry = eventRegistry;
        this.conversionService = conversionService;
    }

    /**
     * Serialize the given data to a String based on the topic's serialization format.
     *
     * @param topic the topic to which the data is being serialized
     * @param data  the data to serialize
     * @return the serialized String, or null if data is null
     */
    public String serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String string) {
            return string;
        }
        if (!eventRegistry.contains(topic)) {
            return jsonUtil.toJson(data);
        }
        return switch (eventRegistry.serialization(topic)) {
            case AVRO -> serializeAvro(data);
            case JSON -> jsonUtil.toJson(data);
        };
    }

    private String serializeAvro(Object data) {
        var avroWriter = new GenericDatumWriter<GenericRecord>();
        var outputStream = new ByteArrayOutputStream();
        var encoder = new EncoderFactory().binaryEncoder(outputStream, null);
        var genericRecord = conversionService.convert(data, GenericRecord.class);
        try {
            avroWriter.setSchema(Objects.requireNonNull(genericRecord).getSchema());
            avroWriter.write(genericRecord, encoder);
            encoder.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize object to Avro", e);
        }
    }
}
