package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.tracing.MessageTracing;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serialization of state that may contain tracing information.
 *
 * <p>Envelope layout: {@code 0xFF 'P' 'T' version, int originsLength, origins (UTF-8, comma-separated), payload}.
 * The first byte can never start a JSON, string or Confluent Avro payload (the latter starts with {@code 0x00}), so
 * a reader can tell enveloped bytes from legacy bytes. Readers are always tolerant; the envelope is only written
 * when tracing is enabled.
 */
final class TracedValueSerde {
    private static final byte[] MAGIC = {(byte) 0xFF, 'P', 'T', 1};

    private TracedValueSerde() {
    }

    /** Serde that writes the envelope and reads both enveloped and legacy bytes. */
    static <V> Serde<TracedValue<V>> writing(Serde<V> inner) {
        Serializer<TracedValue<V>> serializer = (topic, traced) ->
                traced == null ? null : envelope(traced, inner.serializer().serialize(topic, traced.value()));
        Deserializer<TracedValue<V>> deserializer = (topic, bytes) -> read(topic, bytes, inner.deserializer());
        return new SerdeAdapter<>(serializer, deserializer);
    }

    /** Serde for state that carries no tracing information: writes plain bytes, strips an envelope if present. */
    static <V> Serde<V> tolerant(Serde<V> inner) {
        Deserializer<V> deserializer = (topic, bytes) -> {
            if (bytes == null) {
                return null;
            }
            return inner.deserializer().deserialize(topic, hasEnvelope(bytes) ? payload(bytes) : bytes);
        };
        return new SerdeAdapter<>(inner.serializer(), deserializer);
    }

    private static <V> TracedValue<V> read(String topic, byte[] bytes, Deserializer<V> inner) {
        if (bytes == null) {
            return null;
        }
        if (!hasEnvelope(bytes)) {
            return new TracedValue<>(inner.deserialize(topic, bytes), List.of());
        }
        var buffer = ByteBuffer.wrap(bytes, MAGIC.length, bytes.length - MAGIC.length);
        var origins = new byte[buffer.getInt()];
        buffer.get(origins);
        var payload = new byte[buffer.remaining()];
        buffer.get(payload);
        return new TracedValue<>(
                inner.deserialize(topic, payload),
                MessageTracing.decode(new String(origins, StandardCharsets.UTF_8)));
    }

    private static byte[] envelope(TracedValue<?> traced, byte[] payload) {
        var origins = MessageTracing.encode(traced.origins()).getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(MAGIC.length + Integer.BYTES + origins.length + payload.length)
                .put(MAGIC)
                .putInt(origins.length)
                .put(origins)
                .put(payload)
                .array();
    }

    private static byte[] payload(byte[] bytes) {
        var origins = ByteBuffer.wrap(bytes, MAGIC.length, Integer.BYTES).getInt();
        return Arrays.copyOfRange(bytes, MAGIC.length + Integer.BYTES + origins, bytes.length);
    }

    private static boolean hasEnvelope(byte[] bytes) {
        return bytes.length >= MAGIC.length + Integer.BYTES
                && Arrays.equals(bytes, 0, MAGIC.length, MAGIC, 0, MAGIC.length);
    }
}
