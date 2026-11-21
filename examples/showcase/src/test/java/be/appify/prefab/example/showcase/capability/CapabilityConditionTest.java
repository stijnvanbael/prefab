package be.appify.prefab.example.showcase.capability;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityConditionTest {

    private static final VariantCapabilities PUBSUB = VariantCapabilities.parse("test.yml", """
            variant: showcase-pubsub
            unsupported:
              STREAMS:
                reason: no Pub/Sub backend for Prefab Streams
                task: TASK-341
              AVRO:
                reason: schemas are not self-describing
                task: TASK-275
            """);

    private final CapabilityCondition condition = new CapabilityCondition(() -> PUBSUB);

    @Test
    void evaluate_givenUnsupportedCapability_disablesWithVariantReasonAndTask() throws Exception {
        var result = condition.evaluate(methodOf("needsStreams"));

        assertThat(result.isDisabled()).isTrue();
        assertThat(result.getReason().orElseThrow())
                .isEqualTo("showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-341)");
    }

    @Test
    void evaluate_givenSupportedCapability_enables() throws Exception {
        var result = condition.evaluate(methodOf("needsSecurity"));

        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void evaluate_givenSeveralCapabilities_reportsEveryGap() throws Exception {
        var result = condition.evaluate(methodOf("needsStreamsAndAvro"));

        assertThat(result.isDisabled()).isTrue();
        assertThat(result.getReason().orElseThrow()).contains("STREAMS not supported").contains("AVRO not supported");
    }

    @Test
    void evaluate_givenNoRequirement_enables() throws Exception {
        var result = condition.evaluate(methodOf("needsNothing"));

        assertThat(result.isDisabled()).isFalse();
    }

    private static Method methodOf(String methodName) throws NoSuchMethodException {
        return Fixture.class.getDeclaredMethod(methodName);
    }

    @SuppressWarnings("unused")
    static class Fixture {

        @RequiresCapability(Capability.STREAMS)
        void needsStreams() {
        }

        @RequiresCapability(Capability.SECURITY)
        void needsSecurity() {
        }

        @RequiresCapability({Capability.STREAMS, Capability.AVRO})
        void needsStreamsAndAvro() {
        }

        void needsNothing() {
        }
    }
}
