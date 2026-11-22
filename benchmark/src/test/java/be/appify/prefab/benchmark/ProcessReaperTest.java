package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledOnOs(OS.LINUX)
class ProcessReaperTest {
    @TempDir
    Path directory;

    @Test
    void killsBackgroundProcessesLeftInTheWorkspace() throws Exception {
        var workspace = Files.createDirectories(directory.resolve("project/sub"));
        var leftover = new ProcessBuilder("sleep", "300").directory(workspace.toFile()).start();
        var elsewhere = new ProcessBuilder("sleep", "300").directory(directory.toFile()).start();
        try {
            assertThat(ProcessReaper.pidsIn(directory.resolve("project"))).contains(leftover.pid());

            var killed = ProcessReaper.killProcessesIn(directory.resolve("project"));

            assertThat(killed).isEqualTo(1);
            assertThat(leftover.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(elsewhere.isAlive()).isTrue();
        } finally {
            elsewhere.destroyForcibly();
            leftover.destroyForcibly();
        }
    }
}
