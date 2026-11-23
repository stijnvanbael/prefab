package be.appify.prefab.streams.kafka;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TracedValueSerdeTest {
    private static final String TOPIC = "topic";

    @Test
    void writing_shouldRoundTripOrigins() {
        var serde = TracedValueSerde.writing(Serdes.String());

        var bytes = serde.serializer().serialize(TOPIC, new TracedValue<>("payload", List.of("m-1", "m-2")));

        assertThat(serde.deserializer().deserialize(TOPIC, bytes))
                .isEqualTo(new TracedValue<>("payload", List.of("m-1", "m-2")));
    }

    @Test
    void writing_shouldReadLegacyBytesAsNoOrigins() {
        var serde = TracedValueSerde.writing(Serdes.String());

        var traced = serde.deserializer().deserialize(TOPIC, "{\"a\":1}".getBytes(StandardCharsets.UTF_8));

        assertThat(traced).isEqualTo(new TracedValue<>("{\"a\":1}", List.of()));
    }

    @Test
    void writing_shouldHandleNull() {
        var serde = TracedValueSerde.writing(Serdes.String());

        assertThat(serde.serializer().serialize(TOPIC, null)).isNull();
        assertThat(serde.deserializer().deserialize(TOPIC, null)).isNull();
    }

    @Test
    void tolerant_shouldStripEnvelopeWrittenWhileTracingWasEnabled() {
        var envelope = TracedValueSerde.writing(Serdes.String()).serializer()
                .serialize(TOPIC, new TracedValue<>("payload", List.of("m-1")));

        var value = TracedValueSerde.tolerant(Serdes.String()).deserializer().deserialize(TOPIC, envelope);

        assertThat(value).isEqualTo("payload");
    }

    @Test
    void tolerant_shouldWriteUnchangedBytes() {
        var bytes = TracedValueSerde.tolerant(Serdes.String()).serializer().serialize(TOPIC, "payload");

        assertThat(bytes).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
    }
}
