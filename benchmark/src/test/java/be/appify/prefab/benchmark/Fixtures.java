package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Test data for report-related tests. */
final class Fixtures {
    private Fixtures() {
    }

    static BenchmarkTask task(String id) {
        return new BenchmarkTask(id, "Title", "rest-crud", Tier.QUICK, Optional.empty(), Set.of(PrefabModule.POSTGRES),
                "Do it", Duration.ofMinutes(10), 50, new BenchmarkTask.Acceptance(false, 1, List.of(), false, false), Set.of(),
                Path.of("tasks", id));
    }

    static TaskResult result(String taskId, int attempt, boolean passed, FailureCategory category) {
        return result(taskId, Tier.QUICK, attempt, passed, category);
    }

    static TaskResult result(String taskId, Tier tier, int attempt, boolean passed, FailureCategory category) {
        var agent = new Agent.AgentOutcome(Agent.Status.COMPLETED, new Agent.TokenUsage(100, 20, 1000, 50), 12, 3,
                0.25, Duration.ofSeconds(95), false, "Done", Optional.of(Duration.ofSeconds(40)));
        var checks = List.of(new AcceptanceRunner.CheckResult("guardrails", true, "ok", false),
                new AcceptanceRunner.CheckResult("hidden-tests", passed, passed ? "2/2 passed" : "1/2 passed", false));
        var acceptance = new AcceptanceRunner.AcceptanceResult(checks, List.of(), List.of(new Git.ChangedFile('A', "x")),
                new SurefireReports.TestSummary(1, 2, passed ? 0 : 1, 0, List.of()), List.of(), false);
        var failure = passed ? null
                : new TaskResult.Classification(category, "heuristic", "why", List.of("hidden-tests: 1/2 passed"));
        return new TaskResult(taskId, "rest-crud", tier, attempt, passed, agent, acceptance, Duration.ofSeconds(30),
                failure, "logs/" + taskId);
    }

    static RunReport report(String runId, List<TaskResult> results) {
        return new RunReport(runId, Instant.parse("2026-11-21T10:00:00Z"), "claude-code/claude-haiku-5-5",
                "claude-haiku-5-5", "none", "0.13.3", "abc123", 1, results);
    }
}
