package be.appify.prefab.core.annotations;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ComputedTest {
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void computedMethodIsSerializedAsField() {
        var json = jsonMapper.writeValueAsString(new Money(BigDecimal.TEN, "EUR"));

        assertThat(json).contains("\"display\":\"10 EUR\"");
    }

    @Test
    void computedFieldIsIgnoredOnDeserialization() {
        var money = jsonMapper.readValue("""
                {"amount":10,"currency":"EUR","display":"tampered"}""", Money.class);

        assertThat(money).isEqualTo(new Money(BigDecimal.valueOf(10), "EUR"));
    }

    record Money(BigDecimal amount, String currency) {
        @Computed
        public String display() {
            return "%s %s".formatted(amount, currency);
        }
    }
}
