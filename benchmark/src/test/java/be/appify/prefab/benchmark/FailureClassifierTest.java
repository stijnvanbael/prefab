package be.appify.prefab.benchmark;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FailureClassifierTest {
    private static final Agent.AgentOutcome COMPLETED = new Agent.AgentOutcome(Agent.Status.COMPLETED,
            Agent.TokenUsage.NONE, 5, 1, 0, Duration.ofSeconds(10), false, "Done");
    private final BenchmarkTask task = Fixtures.task("rest-create-and-get");

    private static AcceptanceRunner.AcceptanceResult acceptance(List<AcceptanceRunner.CheckResult> checks,
            List<Guardrails.Violation> violations, List<String> buildErrors) {
        return new AcceptanceRunner.AcceptanceResult(checks, violations, List.of(), null, buildErrors, false);
    }

    private static AcceptanceRunner.CheckResult failed(String name) {
        return new AcceptanceRunner.CheckResult(name, false, "failed", false);
    }

    @Test
    void agentErrorsAreInfrastructure() {
        var crashed = new Agent.AgentOutcome(Agent.Status.ERROR, Agent.TokenUsage.NONE, 0, 0, 0, Duration.ZERO, false,
                "API overloaded");

        assertThat(FailureClassifier.classify(task, crashed, acceptance(List.of(failed("hidden-tests")), List.of(), List.of()))
                .category()).isEqualTo(FailureCategory.TEST_OR_INFRASTRUCTURE);
    }

    @Test
    void guardrailFailuresArePlainSpringFallbacks() {
        var violation = new Guardrails.Violation("jpa", Guardrails.Severity.FAIL, "JPA", "src/main/java/X.java", 1);

        assertThat(FailureClassifier.classify(task, COMPLETED,
                acceptance(List.of(failed("guardrails")), List.of(violation), List.of())).category())
                .isEqualTo(FailureCategory.PLAIN_SPRING_FALLBACK);
    }

    @Test
    void unresolvedPrefabErrorsPointAtDiagnostics() {
        var errors = List.of("[ERROR] Fatal error compiling: java.lang.IllegalStateException: Cannot add a NOT NULL column");

        assertThat(FailureClassifier.classify(task, COMPLETED, acceptance(List.of(failed("hidden-tests")), List.of(), errors))
                .category()).isEqualTo(FailureCategory.MISLEADING_DIAGNOSTIC);
    }

    @Test
    void otherCompileErrorsAreApiMisuse() {
        var errors = List.of("[ERROR] /work/src/main/java/com/example/catalog/Book.java:[3,1] cannot find symbol");

        assertThat(FailureClassifier.classify(task, COMPLETED, acceptance(List.of(failed("hidden-tests")), List.of(), errors))
                .category()).isEqualTo(FailureCategory.API_MISUSE);
    }

    @Test
    void wrongBehaviourIsADocGapByDefault() {
        var classification = FailureClassifier.classify(task, COMPLETED,
                acceptance(List.of(failed("hidden-tests")), List.of(), List.of()));

        assertThat(classification.category()).isEqualTo(FailureCategory.DOC_GAP);
        assertThat(classification.source()).isEqualTo("heuristic");
        assertThat(classification.evidence()).contains("hidden-tests: failed");
    }
}
