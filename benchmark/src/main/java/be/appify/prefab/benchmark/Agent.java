package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** Something that works on a benchmark task in a workspace: an AI coding agent, or a stand-in used for validation. */
sealed interface Agent permits ClaudeCodeAgent, ReferenceAgent, NoopAgent {

    /** Short description used in reports, e.g. {@code claude-code/claude-haiku-5-5}. */
    String name();

    /** The model under test, or the agent name for stand-ins. */
    String model();

    /**
     * Works on a task until done, out of turns or out of time.
     *
     * @param run what to work on
     * @return metrics of the run
     */
    AgentOutcome run(AgentRun run);

    /**
     * A single agent run.
     *
     * @param task        the task
     * @param prompt      the full prompt (preamble plus task prompt)
     * @param workspace   project root the agent works in
     * @param logs        directory for transcripts and logs
     * @param environment variables the agent and its builds must see (container names per parallel slot)
     */
    record AgentRun(BenchmarkTask task, String prompt, Path workspace, Path logs, Map<String, String> environment) {
        public AgentRun {
            environment = Map.copyOf(environment);
        }
    }

    /**
     * Metrics of an agent run.
     *
     * @param status        how the run ended
     * @param usage         tokens used
     * @param turns         number of agent turns
     * @param buildAttempts number of Maven invocations by the agent (a proxy for compile-and-fix retries)
     * @param costUsd       cost reported by the agent, 0 when unknown
     * @param duration      wall-clock time
     * @param contaminated  whether the agent looked at the Prefab benchmark sources (hidden tests, references)
     * @param message       final message of the agent or error description
     */
    record AgentOutcome(
            Status status,
            TokenUsage usage,
            int turns,
            int buildAttempts,
            double costUsd,
            Duration duration,
            boolean contaminated,
            String message
    ) {
        static AgentOutcome instant(String message) {
            return new AgentOutcome(Status.COMPLETED, TokenUsage.NONE, 0, 0, 0, Duration.ZERO, false, message);
        }
    }

    /** How an agent run ended. */
    enum Status {
        /** The agent said it was done. */
        COMPLETED,
        /** The agent used all of its turns. */
        MAX_TURNS,
        /** The harness killed the agent after the task timeout. */
        TIMED_OUT,
        /** The agent failed: API error, crash, could not start. */
        ERROR
    }

    /**
     * Token usage summed over a run.
     *
     * @param input         uncached input tokens
     * @param output        output tokens
     * @param cacheRead     input tokens read from the prompt cache
     * @param cacheCreation input tokens written to the prompt cache
     */
    record TokenUsage(long input, long output, long cacheRead, long cacheCreation) {
        static final TokenUsage NONE = new TokenUsage(0, 0, 0, 0);

        long totalInput() {
            return input + cacheRead + cacheCreation;
        }

        TokenUsage plus(TokenUsage other) {
            return new TokenUsage(input + other.input, output + other.output,
                    cacheRead + other.cacheRead, cacheCreation + other.cacheCreation);
        }
    }
}
