package be.appify.prefab.streams.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringKeySerde")
class StringKeySerdeTest {

    @Test
    void shouldSerializeSingleFieldRecordAsPlainUtf8String() {
        var serde = new StringKeySerde<>(SingleFieldKey.class);

        var bytes = serde.serializer().serialize("test-topic", new SingleFieldKey("zone-1"));

        assertThat(new String(bytes, UTF_8)).isEqualTo("zone-1");
    }

    @Test
    void shouldRoundTripSingleFieldRecordFromPlainUtf8String() {
        var serde = new StringKeySerde<>(SingleFieldKey.class);

        var key = serde.deserializer().deserialize("test-topic", "zone-2".getBytes(UTF_8));

        assertThat(key).isEqualTo(new SingleFieldKey("zone-2"));
    }

    @Test
    void shouldSerializeComplexRecordAsJson() {
        var serde = new StringKeySerde<>(ComplexKey.class);

        var bytes = serde.serializer().serialize("test-topic", new ComplexKey("zone-1", "2026-07-01"));

        assertThat(new String(bytes, UTF_8)).contains("\"zone\":\"zone-1\"")
                .contains("\"date\":\"2026-07-01\"");
    }

    @Test
    void shouldRoundTripComplexRecordAsJson() {
        var serde = new StringKeySerde<>(ComplexKey.class);
        var original = new ComplexKey("zone-1", "2026-07-01");

        var bytes = serde.serializer().serialize("test-topic", original);
        var restored = serde.deserializer().deserialize("test-topic", bytes);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void deferredSerdeShouldUseRuntimeTypeStrategy() {
        var serde = new DeferredStringKeySerde<SingleFieldKey>();
        var original = new SingleFieldKey("zone-3");

        var bytes = serde.serializer().serialize("test-topic", original);
        var restored = serde.deserializer().deserialize("test-topic", bytes);

        assertThat(new String(bytes, UTF_8)).isEqualTo("zone-3");
        assertThat(restored).isEqualTo(original);
    }

    record SingleFieldKey(String value) {
    }

    record ComplexKey(String zone, String date) {
    }
}


