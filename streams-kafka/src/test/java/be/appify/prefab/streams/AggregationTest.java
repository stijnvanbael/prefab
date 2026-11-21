package be.appify.prefab.streams;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AggregationTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serialize_withoutCorrelationIds_shouldKeepLegacyShape() throws Exception {
        var json = mapper.writeValueAsString(new Aggregation<>("k", List.of("v")));

        assertThat(json).isEqualTo("{\"key\":\"k\",\"values\":[\"v\"]}");
    }

    @Test
    void deserialize_legacyState_shouldHaveNoCorrelationIds() throws Exception {
        var aggregation = mapper.readValue("{\"key\":\"k\",\"values\":[\"v\"]}", Aggregation.class);

        assertThat(aggregation.correlationIds()).isEmpty();
    }

    @Test
    void serialize_withCorrelationIds_shouldRoundTrip() throws Exception {
        var original = new Aggregation<>("k", List.of("v")).withCorrelation(List.of("m-1", "m-2"), 100);

        var restored = mapper.readValue(mapper.writeValueAsString(original), Aggregation.class);

        assertThat(restored.correlationIds()).containsExactly("m-1", "m-2");
    }

    @Test
    void withCorrelation_overCap_shouldEvictOldest() {
        var aggregation = new Aggregation<>("k", List.of("v"))
                .withCorrelation(List.of("m-1", "m-2"), 2)
                .withCorrelation(List.of("m-3"), 2);

        assertThat(aggregation.correlationIds()).containsExactly("m-2", "m-3");
    }
}
