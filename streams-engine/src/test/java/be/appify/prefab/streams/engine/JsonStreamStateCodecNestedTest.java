package be.appify.prefab.streams.engine;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonStreamStateCodecNestedTest {
    record Value(String key, int count) {}
    record Aggregate(Object value, List<String> correlations) {}
    record Join(List<StreamMessage> messages) {}
    interface Payload {}
    record PayloadValue(String text) implements Payload {}
    record Polymorphic(Payload payload) {}
    record Generic<T>(T payload) {}

    @Test
    void preservesNestedAggregateAndJoinPayloadTypes() {
        var codec = new JsonStreamStateCodec();
        var aggregate = new Aggregate(new Value("a", 1), List.of("id"));
        var join = new Join(List.of(new StreamMessage("id", "a", new Value("a", 1), Map.of(), Instant.EPOCH)));

        assertThat(codec.decodeValue(codec.encodeValue(aggregate))).isEqualTo(aggregate);
        assertThat(codec.decodeValue(codec.encodeValue(join))).isEqualTo(join);
    }

    @Test
    void preservesInterfaceAndGenericPayloads() {
        var codec = new JsonStreamStateCodec();
        var polymorphic = new Polymorphic(new PayloadValue("value"));
        var generic = new Generic<>(new PayloadValue("value"));

        assertThat(codec.decodeValue(codec.encodeValue(polymorphic))).isEqualTo(polymorphic);
        assertThat(codec.decodeValue(codec.encodeValue(generic))).isEqualTo(generic);
    }

    @Test
    void readsLegacyStateWithoutAnEncodingMarker() {
        var codec = new JsonStreamStateCodec();
        var json = "{\"type\":\"" + Value.class.getName() + "\",\"value\":{\"key\":\"a\",\"count\":1}}";

        assertThat(codec.decodeValue(json.getBytes(StandardCharsets.UTF_8))).isEqualTo(new Value("a", 1));
    }
}
