package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnOs(OS.WINDOWS)
class ProcessRunnerTest {
    @TempDir
    Path directory;

    @Test
    void recordsWhenEachOutputLineArrived() throws Exception {
        var output = directory.resolve("out.jsonl");
        var times = directory.resolve("out.times");

        var result = ProcessRunner.runRecordingLineTimes(List.of("sh", "-c", "echo first; sleep 0.3; echo second"),
                ProcessRunner.ProcessOptions.of(directory, output, Duration.ofSeconds(30)), times);

        assertThat(result.succeeded()).isTrue();
        assertThat(Files.readAllLines(output)).containsExactly("first", "second");
        var arrivals = Files.readAllLines(times).stream().map(Long::parseLong).toList();
        assertThat(arrivals).hasSize(2);
        assertThat(arrivals.get(1) - arrivals.get(0)).isGreaterThanOrEqualTo(250);
    }
}
