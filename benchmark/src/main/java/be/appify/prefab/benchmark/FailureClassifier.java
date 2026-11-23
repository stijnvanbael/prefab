package be.appify.prefab.benchmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggests a {@link FailureCategory} from the evidence of a failed attempt. Heuristics are a starting point:
 * {@code triage} (an LLM reading the diff and logs) or a manual entry in {@code classifications.yml} can override them.
 */
final class FailureClassifier {

    private FailureClassifier() {
    }

    static TaskResult.Classification classify(BenchmarkTask task, Agent.AgentOutcome agent,
            AcceptanceRunner.AcceptanceResult acceptance) {
        var evidence = evidence(agent, acceptance);
        if (agent.status() == Agent.Status.ERROR || acceptance.dockerUnavailable()
                || acceptance.checks().stream().anyMatch(check -> !check.passed() && check.infrastructure())) {
            return heuristic(FailureCategory.TEST_OR_INFRASTRUCTURE,
                    "the agent crashed or the environment failed: " + agent.message(), evidence);
        }
        if (failed(acceptance, "guardrails")) {
            return heuristic(FailureCategory.PLAIN_SPRING_FALLBACK,
                    "the agent wrote code Prefab is meant to generate", evidence);
        }
        var prefabErrors = acceptance.buildErrors().stream()
                .filter(line -> line.contains("Prefab") || line.contains("be.appify.prefab.processor")
                        || line.contains("Fatal error compiling"))
                .toList();
        if (!prefabErrors.isEmpty()) {
            return heuristic(FailureCategory.MISLEADING_DIAGNOSTIC,
                    "the build ends with a Prefab processor error the agent did not resolve", evidence);
        }
        var hiddenSources = AcceptanceRunner.hiddenTestClasses(task).stream()
                .map(name -> name.substring(name.lastIndexOf('.') + 1) + ".java")
                .toList();
        var hiddenCompileErrors = acceptance.buildErrors().stream()
                .anyMatch(line -> hiddenSources.stream().anyMatch(line::contains));
        if (hiddenCompileErrors) {
            return heuristic(FailureCategory.API_MISUSE,
                    "the hidden tests do not compile against the generated API: names or shapes differ from the prompt",
                    evidence);
        }
        if (!acceptance.buildErrors().isEmpty()) {
            return heuristic(FailureCategory.API_MISUSE, "the agent left the project not compiling", evidence);
        }
        return heuristic(FailureCategory.DOC_GAP,
                "the project builds but behaves differently than asked; review the diff to confirm", evidence);
    }

    private static boolean failed(AcceptanceRunner.AcceptanceResult acceptance, String checkName) {
        return acceptance.checks().stream().anyMatch(check -> check.name().equals(checkName) && !check.passed());
    }

    private static TaskResult.Classification heuristic(FailureCategory category, String rationale, List<String> evidence) {
        return new TaskResult.Classification(category, "heuristic", rationale, evidence);
    }

    static List<String> evidence(Agent.AgentOutcome agent, AcceptanceRunner.AcceptanceResult acceptance) {
        var evidence = new ArrayList<String>();
        if (agent.status() != Agent.Status.COMPLETED) {
            evidence.add("agent: " + agent.status() + " - " + agent.message());
        }
        acceptance.checks().stream()
                .filter(check -> !check.passed())
                .forEach(check -> evidence.add(check.name() + ": " + check.detail()));
        acceptance.violations().forEach(violation -> evidence.add("guardrail " + violation.rule() + " ("
                + violation.severity() + ") " + violation.file() + (violation.line() > 0 ? ":" + violation.line() : "")));
        acceptance.buildErrors().stream().limit(10).forEach(line -> evidence.add("build: " + line));
        return evidence;
    }
}
