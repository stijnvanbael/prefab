package be.appify.prefab.benchmark;

import java.time.Duration;
import java.util.List;

/**
 * The outcome of one task attempt.
 *
 * @param taskId             task id
 * @param category           task category
 * @param tier               task tier; {@link Tier#QUICK} for reports written before tiers existed
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
        Tier tier,
        int attempt,
        boolean passed,
        Agent.AgentOutcome agent,
        AcceptanceRunner.AcceptanceResult acceptance,
        Duration acceptanceDuration,
        Classification failure,
        String logs
) {
    TaskResult {
        tier = tier == null ? Tier.QUICK : tier;
    }

    String key() {
        return taskId + "#" + attempt;
    }

    TaskResult withFailure(Classification failure) {
        return new TaskResult(taskId, category, tier, attempt, passed, agent, acceptance, acceptanceDuration, failure,
                logs);
    }

    /**
     * Files in which the agent overrode a generated class or hand-wrote what Prefab generates (controllers,
     * repositories, persistence code, message listeners, JPA entities), counted from the guardrail violations. Rules a
     * task allows are not counted.
     */
    long overrides() {
        return acceptance.violations().stream()
                .filter(violation -> Guardrails.OVERRIDE_RULES.contains(violation.rule()))
                .map(Guardrails.Violation::file)
                .distinct()
                .count();
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
