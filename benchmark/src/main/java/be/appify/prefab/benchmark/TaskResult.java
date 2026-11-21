package be.appify.prefab.benchmark;

import java.time.Duration;
import java.util.List;

/**
 * The outcome of one task attempt.
 *
 * @param taskId             task id
 * @param category           task category
 * @param attempt            1-based repetition number
 * @param passed             whether every acceptance check passed
 * @param agent              agent metrics
 * @param acceptance         acceptance checks
 * @param acceptanceDuration time spent on acceptance builds
 * @param failure            classification, {@code null} when passed
 * @param logs               directory with transcript, diff and build logs
 */
record TaskResult(
        String taskId,
        String category,
        int attempt,
        boolean passed,
        Agent.AgentOutcome agent,
        AcceptanceRunner.AcceptanceResult acceptance,
        Duration acceptanceDuration,
        Classification failure,
        String logs
) {
    String key() {
        return taskId + "#" + attempt;
    }

    TaskResult withFailure(Classification failure) {
        return new TaskResult(taskId, category, attempt, passed, agent, acceptance, acceptanceDuration, failure, logs);
    }

    /**
     * Why an attempt failed.
     *
     * @param category  the category
     * @param source    who classified it: {@code heuristic}, {@code triage:<model>} or {@code manual}
     * @param rationale short explanation
     * @param evidence  supporting lines (failed checks, build errors)
     */
    record Classification(FailureCategory category, String source, String rationale, List<String> evidence) {
        Classification {
            evidence = List.copyOf(evidence);
        }
    }
}
