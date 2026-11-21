package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs Maven in a workspace. The executable can be overridden with the {@code BENCHMARK_MVN} environment variable.
 *
 * @param executable  Maven executable
 * @param environment variables for the build (container names of the parallel slot)
 * @param timeout     maximum build time
 */
record Maven(String executable, Map<String, String> environment, Duration timeout) {
    /** Build logs above this size keep only their head and tail; a looping consumer can log gigabytes. */
    static final long MAX_LOG_BYTES = 20L * 1024 * 1024;

    Maven {
        environment = Map.copyOf(environment);
    }

    static Maven create(Map<String, String> environment) {
        var executable = System.getenv().getOrDefault("BENCHMARK_MVN", "mvn");
        return new Maven(executable, environment, Duration.ofMinutes(20));
    }

    /**
     * Runs {@code mvn test}.
     *
     * @param workspace   project root
     * @param testClasses simple or qualified test class names to run; empty runs all tests
     * @param log         build log, appended
     * @return the process outcome
     */
    ProcessRunner.ProcessResult test(Path workspace, List<String> testClasses, Path log) {
        var command = new ArrayList<>(List.of(executable, "-B", "-ntp", "test"));
        if (!testClasses.isEmpty()) {
            command.add("-Dtest=" + String.join(",", testClasses));
            command.add("-Dsurefire.failIfNoSpecifiedTests=false");
        }
        FileTrees.append(log, "\n$ " + String.join(" ", command) + "\n");
        var result = ProcessRunner.run(command,
                ProcessRunner.ProcessOptions.of(workspace, log, timeout).withEnvironment(environment));
        FileTrees.truncateMiddle(log, MAX_LOG_BYTES);
        return result;
    }
}
