package com.example.streams;

import be.appify.prefab.streams.engine.InMemoryTopologyTestHarness;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeartbeatPunctuationAcceptanceTest {
    @Test
    void emitsOneHeartbeatPerElapsedIntervalWithoutTriggeringOnInput() {
        var harness = new InMemoryTopologyTestHarness("heartbeat-benchmark");
        harness.streams().from(Pulse.class).process(new HeartbeatProcessor()).to("heartbeats");

        try (var topology = harness.start()) {
            var heartbeats = topology.output("heartbeats", Heartbeat.class);
            topology.advanceWallClock(Duration.ofMillis(999));
            assertThat(heartbeats.isEmpty()).isTrue();
            topology.input(Pulse.class).send(new Pulse("pulse-1", "ready"));
            assertThat(heartbeats.isEmpty()).isTrue();

            topology.advanceWallClock(Duration.ofMillis(1));
            var first = heartbeats.value();
            assertThat(first.id()).isEqualTo("heartbeat");
            assertThat(first.sequence()).isEqualTo(1);
            assertThat(first.emittedAt()).isNotNull();

            topology.advanceWallClock(Duration.ofSeconds(1));
            assertThat(heartbeats.values()).containsExactly(
                    new Heartbeat("heartbeat", 2, first.emittedAt().plusSeconds(1)));
        }
    }
}
