package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * One benchmark task: a starting project, a natural-language prompt and the acceptance criteria the harness checks
 * once the agent is done. Tasks live in {@code benchmark/tasks/<id>/}; see {@code backlog/docs/benchmark.md}.
 *
 * @param id         directory name, unique and stable (used in reports)
 * @param title      one-line summary
 * @param category   use case the task exercises, e.g. {@code rest-crud} or {@code events-handling}
 * @param modules    Prefab modules the starting project depends on
 * @param prompt     what the agent is asked to do
 * @param timeout    maximum agent run time
 * @param maxTurns   maximum number of agent turns
 * @param acceptance how the result is checked
 * @param guardrails which guardrail rules this task allows
 * @param directory  the task directory
 */
record BenchmarkTask(
        String id,
        String title,
        String category,
        Set<PrefabModule> modules,
        String prompt,
        Duration timeout,
        int maxTurns,
        Acceptance acceptance,
        Set<String> guardrails,
        Path directory
) {
    BenchmarkTask {
        modules = Set.copyOf(modules);
        guardrails = Set.copyOf(guardrails);
    }

    Path starter() {
        return directory.resolve("starter");
    }

    Path hidden() {
        return directory.resolve("hidden");
    }

    Path reference() {
        return directory.resolve("reference");
    }

    Path mutant(String name) {
        return directory.resolve("mutants").resolve(name);
    }

    boolean hasHiddenTests() {
        return Files.isDirectory(hidden());
    }

    /**
     * How a task's result is accepted.
     *
     * @param agentTests   the agent must write tests: they must pass on its own code and fail on every mutant
     * @param minAgentTests minimum number of agent-written tests that must run
     * @param mutants      names of directories under {@code mutants/} that overlay a bug onto {@code src/main}
     * @param mainReadOnly the agent must not change {@code src/main} (for test-writing tasks)
     * @param unitTestsOnly the agent's tests must not start a Spring context ({@code @IntegrationTest},
     *                      {@code @SpringBootTest})
     */
    record Acceptance(boolean agentTests, int minAgentTests, List<String> mutants, boolean mainReadOnly,
            boolean unitTestsOnly) {
        Acceptance {
            mutants = List.copyOf(mutants);
        }
    }
}
