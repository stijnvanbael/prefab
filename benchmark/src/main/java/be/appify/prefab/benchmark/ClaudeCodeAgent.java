package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs Claude Code headless ({@code claude -p}) in the workspace.
 *
 * <p>Only project settings are loaded ({@code --setting-sources project}), so the user's own CLAUDE.md, settings and
 * hooks do not influence the result; the context setup's instructions file is the only guidance. Web access is
 * disabled so the agent cannot look up Prefab documentation online, and permissions are bypassed because the run is
 * unattended in a throw-away workspace.
 *
 * @param model       model id or alias passed to {@code --model}
 * @param executable  the CLI to run, normally {@code claude}
 * @param extraArgs   additional CLI arguments
 * @param forbidden   strings that mark a tool call as looking at the benchmark sources
 * @param isolation   what the agent must not see
 */
record ClaudeCodeAgent(String model, String executable, List<String> extraArgs, List<String> forbidden,
        Isolation isolation) implements Agent {
    /** Variables that make the CLI believe it runs nested inside another Claude Code session. */
    private static final List<String> REMOVED_ENVIRONMENT = List.of("CLAUDECODE", "CLAUDE_CODE_ENTRYPOINT");
    private static final Duration GRACE = Duration.ofSeconds(30);

    ClaudeCodeAgent {
        extraArgs = List.copyOf(extraArgs);
        forbidden = List.copyOf(forbidden);
    }

    @Override
    public String name() {
        return "claude-code/" + model;
    }

    @Override
    public AgentOutcome run(AgentRun run) {
        var transcript = run.logs().resolve("transcript.jsonl");
        var command = new ArrayList<>(List.of(
                executable, "-p", run.prompt(),
                "--model", model,
                "--output-format", "stream-json", "--verbose",
                "--max-turns", Integer.toString(run.task().maxTurns()),
                "--permission-mode", "bypassPermissions",
                "--setting-sources", "project",
                "--no-session-persistence",
                "--disallowedTools", "WebFetch,WebSearch"));
        command.addAll(extraArgs);
        var stage = createTempDirectory("prefab-benchmark-stage-");
        // Claude Code keeps tool output in its temp directory (/tmp/claude-<uid> by default). That directory may be
        // hidden by the isolation, so every attempt gets its own, outside the hidden paths.
        var agentTemp = createTempDirectory("prefab-benchmark-agent-tmp-");
        var environment = new java.util.HashMap<>(run.environment());
        environment.put("CLAUDE_CODE_TMPDIR", agentTemp.toString());
        var options = new ProcessRunner.ProcessOptions(
                run.workspace(), environment, REMOVED_ENVIRONMENT,
                transcript, run.logs().resolve("agent-stderr.log"), run.task().timeout().plus(GRACE));
        try {
            return outcome(run, transcript, ProcessRunner.run(isolation.wrap(command, run.workspace(), stage), options));
        } finally {
            FileTrees.delete(stage);
            FileTrees.delete(agentTemp);
        }
    }

    private AgentOutcome outcome(AgentRun run, Path transcript, ProcessRunner.ProcessResult process) {
        var parsed = TranscriptParser.parse(transcript, forbidden);
        var status = process.timedOut() ? Status.TIMED_OUT
                : parsed.complete() ? parsed.status()
                : Status.ERROR;
        var message = parsed.complete() ? parsed.finalMessage()
                : process.timedOut() ? "killed after " + run.task().timeout()
                : "agent exited with code " + process.exitCode() + " without a result, see agent-stderr.log";
        return new AgentOutcome(status, parsed.usage(), parsed.turns(), parsed.buildAttempts(), parsed.costUsd(),
                process.duration(), parsed.contaminated(), message);
    }

    private static Path createTempDirectory(String prefix) {
        try {
            return java.nio.file.Files.createTempDirectory(Path.of("/tmp"), prefix);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    static List<String> forbiddenMarkers(Path repository) {
        return List.of(repository.toAbsolutePath().normalize().toString(), "benchmark/tasks/");
    }
}
