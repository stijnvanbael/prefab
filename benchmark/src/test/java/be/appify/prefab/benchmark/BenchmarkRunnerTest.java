package be.appify.prefab.benchmark;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkRunnerTest {

    @Test
    void slotsAndRunsGetTheirOwnContainersAndNetworks() {
        var first = BenchmarkRunner.slotEnvironment("a1", 1);
        var second = BenchmarkRunner.slotEnvironment("a1", 2);
        var otherRun = BenchmarkRunner.slotEnvironment("b2", 1);

        assertThat(first).containsEntry("PREFAB_TEST_KAFKA_CONTAINER_NAME", "prefab_bench_a1_1_kafka")
                .containsEntry("PREFAB_TEST_KAFKA_NETWORK_NAME", "prefab_bench_a1_1_kafka")
                .containsEntry("TESTCONTAINERS_REUSE_ENABLE", "false");
        assertThat(first.values()).doesNotContainAnyElementsOf(
                second.values().stream().filter(value -> value.startsWith("prefab_")).toList());
        assertThat(first.values()).doesNotContainAnyElementsOf(
                otherRun.values().stream().filter(value -> value.startsWith("prefab_")).toList());
    }
}
