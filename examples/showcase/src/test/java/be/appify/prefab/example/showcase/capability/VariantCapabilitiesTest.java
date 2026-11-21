package be.appify.prefab.example.showcase.capability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariantCapabilitiesTest {

    @Test
    void parse_givenGaps_returnsThemInCapabilityOrder() {
        var capabilities = VariantCapabilities.parse("test.yml", """
                variant: showcase-pubsub
                unsupported:
                  STREAMS:
                    reason: no Pub/Sub backend for Prefab Streams
                    task: TASK-341
                  FULL_TEXT_SEARCH:
                    reason: no full-text index
                    task: TASK-337.03
                """);

        assertThat(capabilities.variant()).isEqualTo("showcase-pubsub");
        assertThat(capabilities.gaps()).extracting(CapabilityGap::capability)
                .containsExactly(Capability.FULL_TEXT_SEARCH, Capability.STREAMS);
        assertThat(capabilities.supports(Capability.STREAMS)).isFalse();
        assertThat(capabilities.supports(Capability.AVRO)).isTrue();
    }

    @Test
    void parse_givenNoUnsupportedSection_supportsEverything() {
        var capabilities = VariantCapabilities.parse("test.yml", "variant: showcase\n");

        assertThat(capabilities.gaps()).isEmpty();
    }

    @Test
    void skipMessage_describesVariantCapabilityReasonAndTask() {
        var capabilities = VariantCapabilities.parse("test.yml", """
                variant: showcase-pubsub
                unsupported:
                  STREAMS:
                    reason: no Pub/Sub backend for Prefab Streams
                    task: TASK-341
                """);

        assertThat(capabilities.skipMessage(capabilities.gaps().getFirst()))
                .isEqualTo("showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-341)");
    }

    @Test
    void parse_givenUnknownCapability_fails() {
        assertThatThrownBy(() -> VariantCapabilities.parse("test.yml", """
                variant: showcase-x
                unsupported:
                  TELEPATHY:
                    reason: not invented yet
                    task: TASK-1
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown capability 'TELEPATHY'");
    }

    @Test
    void parse_givenGapWithoutTask_fails() {
        assertThatThrownBy(() -> VariantCapabilities.parse("test.yml", """
                variant: showcase-x
                unsupported:
                  STREAMS:
                    reason: no backend
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STREAMS needs the backlog 'task'");
    }

    @Test
    void parse_givenTaskThatIsNoTaskId_fails() {
        assertThatThrownBy(() -> VariantCapabilities.parse("test.yml", """
                variant: showcase-x
                unsupported:
                  STREAMS:
                    reason: no backend
                    task: later
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'later' is not a backlog task ID");
    }

    @Test
    void parse_givenGapWithoutReason_fails() {
        assertThatThrownBy(() -> VariantCapabilities.parse("test.yml", """
                variant: showcase-x
                unsupported:
                  STREAMS:
                    task: TASK-341
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STREAMS needs a 'reason'");
    }

    @Test
    void parse_givenMissingVariant_fails() {
        assertThatThrownBy(() -> VariantCapabilities.parse("test.yml", "unsupported: {}\n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'variant' is required");
    }

    @Test
    void parse_givenUnexpectedKey_fails() {
        assertThatThrownBy(() -> VariantCapabilities.parse("test.yml", """
                variant: showcase-x
                unsupportd: {}
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unexpected key 'unsupportd'");
    }
}
