package be.appify.prefab.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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

    /**
     * Runs a command like {@link #run}, and also records when each line of its standard output arrived: one line per
     * output line in {@code times}, holding the milliseconds since the start. Use it for output without timestamps of its
     * own, such as the agent transcript.
     *
     * @param command the command, never empty
     * @param options where to run it and where its output goes
     * @param times   file that receives the arrival times (appended)
     * @return the outcome
     */
    static ProcessResult runRecordingLineTimes(List<String> command, ProcessOptions options, Path times) {
        var builder = new ProcessBuilder(command).directory(options.directory().toFile());
        builder.environment().putAll(options.environment());
        options.removedEnvironment().forEach(builder.environment()::remove);
        createParent(options.stdout());
        createParent(times);
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
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
            var copier = Thread.ofVirtual().start(() -> copyLines(process, options.stdout(), times, start));
            var finished = process.waitFor(options.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                destroy(process);
            }
            // Background processes the command started can keep the pipe open; don't wait for them.
            if (!copier.join(Duration.ofSeconds(10))) {
                process.getInputStream().close();
                copier.join(Duration.ofSeconds(5));
            }
            return new ProcessResult(finished ? process.exitValue() : -1, !finished, Duration.between(start, Instant.now()));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + String.join(" ", command), e);
        }
    }

    private static void copyLines(Process process, Path stdout, Path times, Instant start) {
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             var output = Files.newBufferedWriter(stdout, StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             var arrivals = Files.newBufferedWriter(times, StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.write(line);
                output.newLine();
                arrivals.write(Long.toString(Duration.between(start, Instant.now()).toMillis()));
                arrivals.newLine();
                output.flush();
                arrivals.flush();
            }
        } catch (IOException e) {
            // The stream is closed on purpose when background processes keep it open after the command ended; the
            // lines read so far are kept, and the parser ignores arrival times that do not match the transcript.
            FileTrees.append(times.resolveSibling("process-runner.log"),
                    "stopped copying output of the process: " + e + System.lineSeparator());
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
