package be.appify.prefab.core.serialization;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import java.io.IOException;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;

/** Reads broker payloads that use the registered current schema, without a writer-schema envelope (TASK-275). */
public final class ConcreteAvroSupport {
    private ConcreteAvroSupport() {}

    /**
     * Reads an Avro payload against the locally registered concrete schema.
     * @param data binary payload
     * @param type concrete binding
     * @param registry schema and converter registry
     * @param <E> payload type
     * @return the concrete payload
     */
    public static <E> E read(byte[] data, EventType<E> type, EventRegistry registry) {
        var schema = registry.readerSchemaFor(type).orElseThrow(
                () -> new IllegalArgumentException("No Avro schema registered for " + type));
        try {
            var decoder = DecoderFactory.get().binaryDecoder(data, null);
            var record = new GenericDatumReader<GenericRecord>(schema).read(null, decoder);
            return registry.fromAvro(type, record);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read concrete Avro payload for " + type, e);
        }
    }
}
