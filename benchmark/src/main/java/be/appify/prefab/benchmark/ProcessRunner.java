package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs external commands (Maven, git, the agent CLI) with a timeout, an explicit environment and output captured in
 * log files, so every step of a benchmark run can be inspected afterwards.
 */
final class ProcessRunner {

    private ProcessRunner() {
    }

    /**
     * Runs a command and waits for it to finish.
     *
     * @param command the command, never empty
     * @param options where to run it and where its output goes
     * @return the outcome
     */
    static ProcessResult run(List<String> command, ProcessOptions options) {
        var builder = new ProcessBuilder(command).directory(options.directory().toFile());
        builder.environment().putAll(options.environment());
        options.removedEnvironment().forEach(builder.environment()::remove);
        createParent(options.stdout());
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(options.stdout().toFile()));
        if (options.stderr() == null) {
            builder.redirectErrorStream(true);
        } else {
            createParent(options.stderr());
            builder.redirectError(ProcessBuilder.Redirect.appendTo(options.stderr().toFile()));
        }
        builder.redirectInput(ProcessBuilder.Redirect.from(nullDevice()));
        var start = Instant.now();
        try {
            var process = builder.start();
            var finished = process.waitFor(options.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                destroy(process);
            }
            return new ProcessResult(finished ? process.exitValue() : -1, !finished, Duration.between(start, Instant.now()));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + String.join(" ", command), e);
        }
    }

    private static void destroy(Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        process.waitFor(30, TimeUnit.SECONDS);
    }

    private static java.io.File nullDevice() {
        return Path.of(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null").toFile();
    }

    private static void createParent(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * How to run a process.
     *
     * @param directory          working directory
     * @param environment        variables added to the inherited environment
     * @param removedEnvironment inherited variables to remove
     * @param stdout             file that receives standard output (appended)
     * @param stderr             file that receives standard error, or {@code null} to merge it into stdout
     * @param timeout            maximum run time; the process tree is killed when it is exceeded
     */
    record ProcessOptions(
            Path directory,
            Map<String, String> environment,
            List<String> removedEnvironment,
            Path stdout,
            Path stderr,
            Duration timeout
    ) {
        ProcessOptions {
            environment = Map.copyOf(environment);
            removedEnvironment = List.copyOf(removedEnvironment);
        }

        static ProcessOptions of(Path directory, Path log, Duration timeout) {
            return new ProcessOptions(directory, Map.of(), List.of(), log, null, timeout);
        }

        ProcessOptions withEnvironment(Map<String, String> environment) {
            return new ProcessOptions(directory, environment, removedEnvironment, stdout, stderr, timeout);
        }
    }

    /**
     * Outcome of a process.
     *
     * @param exitCode exit code, or -1 when the process timed out
     * @param timedOut whether the timeout was hit
     * @param duration wall-clock time
     */
    record ProcessResult(int exitCode, boolean timedOut, Duration duration) {
        boolean succeeded() {
            return !timedOut && exitCode == 0;
        }
    }
}
