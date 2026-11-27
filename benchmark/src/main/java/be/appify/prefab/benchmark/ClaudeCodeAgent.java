package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Runs Claude Code headless ({@code claude -p}) in the workspace.
 *
 * <p>Only project settings are loaded ({@code --setting-sources project}), so the user's own CLAUDE.md, settings and
 * hooks do not influence the result; the context setup's instructions file is the only guidance. Web access is
 * disabled so the agent cannot look up Prefab documentation online, and permissions are bypassed because the run is
 * unattended in a throw-away workspace. Deny rules still apply in that mode, which is what the {@link Isolation} relies
 * on where it hides paths through Claude Code's sandbox; its arguments are kept in the attempt's
 * {@code isolation-arguments.txt}.
 *
 * @param profile     the model to drive, where it is served and how patient to be with it
 * @param executable  the CLI to run, normally {@code claude}
 * @param forbidden   strings that mark a tool call as looking at the benchmark sources
 * @param isolation   what the agent must not see
 */
record ClaudeCodeAgent(ModelProfile profile, String executable, List<String> forbidden, Isolation isolation)
        implements Agent {
    /** Variables that make the CLI believe it runs nested inside another Claude Code session. */
    private static final List<String> REMOVED_ENVIRONMENT = List.of("CLAUDECODE", "CLAUDE_CODE_ENTRYPOINT");
    private static final Duration GRACE = Duration.ofSeconds(30);
    /**
     * Parent of every attempt's Claude Code temp directory. Claude Code's sandbox hides it except for the attempt's own
     * directory, so parallel attempts cannot read each other's tool output.
     */
    static final Path AGENT_TEMP_ROOT = Path.of("/tmp/prefab-benchmark-agent");

    ClaudeCodeAgent {
        forbidden = List.copyOf(forbidden);
        if (isolation instanceof Isolation.ClaudeSandbox && profile.args().contains("--settings")) {
            throw new IllegalArgumentException("Model profile '" + profile.name() + "' passes --settings, which would"
                    + " replace the sandbox settings that hide the repository from the agent; remove them from the"
                    + " profile (the harness sets up the sandbox itself)");
        }
    }

    @Override
    public String model() {
        return profile.model();
    }

    @Override
    public String name() {
        return "claude-code/" + profile.model();
    }

    @Override
    public AgentOutcome run(AgentRun run) {
        var transcript = run.logs().resolve("transcript.jsonl");
        var command = new ArrayList<>(List.of(
                executable, "-p", run.prompt(),
                "--model", profile.model(),
                "--output-format", "stream-json", "--verbose",
                "--max-turns", Integer.toString(run.task().maxTurns()),
                "--permission-mode", "bypassPermissions",
                "--setting-sources", "project",
                "--no-session-persistence",
                "--disallowedTools", "WebFetch,WebSearch"));
        command.addAll(profile.args());
        var stage = createTempDirectory(Path.of("/tmp"), "prefab-benchmark-stage-");
        // Claude Code keeps tool output in its temp directory (/tmp/claude-<uid> by default). That directory may be
        // hidden by the isolation, so every attempt gets its own, outside the hidden paths.
        var agentTemp = createTempDirectory(AGENT_TEMP_ROOT, "attempt-");
        var isolationArguments = isolation.agentArguments(run.workspace(), agentTemp);
        command.addAll(isolationArguments);
        if (!isolationArguments.isEmpty()) {
            FileTrees.writeString(run.logs().resolve("isolation-arguments.txt"), String.join("\n", isolationArguments));
        }
        var environment = new HashMap<>(run.environment());
        environment.putAll(profile.env());
        environment.put("CLAUDE_CODE_TMPDIR", agentTemp.toString());
        var timeout = run.task().timeout().multipliedBy(profile.timeoutFactor());
        var options = new ProcessRunner.ProcessOptions(
                run.workspace(), environment, REMOVED_ENVIRONMENT,
                transcript, run.logs().resolve("agent-stderr.log"), timeout.plus(GRACE));
        try {
            return outcome(timeout, transcript, ProcessRunner.runRecordingLineTimes(
                    isolation.wrap(command, run.workspace(), stage), options, TranscriptParser.timesFile(transcript)));
        } finally {
            FileTrees.delete(stage);
            FileTrees.delete(agentTemp);
        }
    }

    private AgentOutcome outcome(Duration timeout, Path transcript, ProcessRunner.ProcessResult process) {
        var parsed = TranscriptParser.parse(transcript, forbidden);
        var status = process.timedOut() ? Status.TIMED_OUT
                : parsed.complete() ? parsed.status()
                : Status.ERROR;
        var message = parsed.complete() ? parsed.finalMessage()
                : process.timedOut() ? "killed after " + timeout
                : "agent exited with code " + process.exitCode() + " without a result, see agent-stderr.log";
        return new AgentOutcome(status, parsed.usage(), parsed.turns(), parsed.buildAttempts(), parsed.costUsd(),
                process.duration(), parsed.contaminated(), message, parsed.firstGreenBuild());
    }

    private static Path createTempDirectory(Path parent, String prefix) {
        try {
            return Files.createTempDirectory(Files.createDirectories(parent), prefix);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create a temp directory in " + parent, e);
        }
    }

    static List<String> forbiddenMarkers(Path repository) {
        return List.of(repository.toAbsolutePath().normalize().toString(), "benchmark/tasks/");
    }
}
