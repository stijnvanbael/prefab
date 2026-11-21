package be.appify.prefab.benchmark;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Loads benchmark tasks from {@code benchmark/tasks}. Each task directory contains a {@code task.yml} and a
 * {@code prompt.md}, plus {@code starter/}, {@code hidden/}, {@code reference/} and optionally {@code mutants/}.
 */
final class TaskCatalog {
    static final int DEFAULT_TIMEOUT_MINUTES = 30;
    static final int DEFAULT_MAX_TURNS = 100;

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final Path tasksDirectory;

    TaskCatalog(Path tasksDirectory) {
        this.tasksDirectory = tasksDirectory;
    }

    /** All tasks, sorted by id. */
    List<BenchmarkTask> all() {
        try (Stream<Path> directories = Files.list(tasksDirectory)) {
            return directories
                    .filter(directory -> Files.isRegularFile(directory.resolve("task.yml")))
                    .sorted()
                    .map(TaskCatalog::load)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list tasks in " + tasksDirectory, e);
        }
    }

    /**
     * Tasks whose id or category matches one of the glob patterns, or all tasks when there are no patterns.
     *
     * @param patterns glob patterns such as {@code rest-*} or {@code events-publish-on-create}
     * @return the matching tasks, sorted by id
     * @throws IllegalArgumentException if a pattern matches no task, which is almost always a typo
     */
    List<BenchmarkTask> select(List<String> patterns) {
        var tasks = all();
        if (patterns.isEmpty()) {
            return tasks;
        }
        var selected = new ArrayList<BenchmarkTask>();
        for (var pattern : patterns) {
            var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            var matches = tasks.stream()
                    .filter(task -> matcher.matches(Path.of(task.id())) || matcher.matches(Path.of(task.category())))
                    .toList();
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("No task matches '" + pattern + "'");
            }
            matches.stream().filter(task -> !selected.contains(task)).forEach(selected::add);
        }
        return selected.stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }

    static BenchmarkTask load(Path directory) {
        var id = directory.getFileName().toString();
        try {
            var spec = YAML.readValue(directory.resolve("task.yml").toFile(), TaskSpec.class);
            var prompt = Files.readString(directory.resolve("prompt.md")).strip();
            var task = spec.toTask(id, prompt, directory);
            validate(task);
            return task;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read task " + id, e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid task " + id + ": " + e.getMessage(), e);
        }
    }

    private static void validate(BenchmarkTask task) {
        if (!Files.isDirectory(task.reference())) {
            throw new IllegalArgumentException("missing reference/ solution, needed to validate the task");
        }
        if (!task.hasHiddenTests() && !task.acceptance().agentTests()) {
            throw new IllegalArgumentException("needs hidden/ tests or acceptance.agentTests");
        }
        for (var mutant : task.acceptance().mutants()) {
            if (!Files.isDirectory(task.mutant(mutant))) {
                throw new IllegalArgumentException("missing mutant directory mutants/" + mutant);
            }
        }
        var unknownRules = task.guardrails().stream().filter(rule -> !Guardrails.RULE_IDS.contains(rule)).toList();
        if (!unknownRules.isEmpty()) {
            throw new IllegalArgumentException("unknown guardrail rules " + unknownRules + ", known: " + Guardrails.RULE_IDS);
        }
    }

    record TaskSpec(
            String title,
            String category,
            List<PrefabModule> modules,
            Integer timeoutMinutes,
            Integer maxTurns,
            AcceptanceSpec acceptance,
            @JsonProperty("allowGuardrails") List<String> allowGuardrails,
            String notes
    ) {
        BenchmarkTask toTask(String id, String prompt, Path directory) {
            Objects.requireNonNull(title, "title is required");
            Objects.requireNonNull(category, "category is required");
            var acceptanceSpec = acceptance == null ? new AcceptanceSpec(null, null, null, null, null) : acceptance;
            return new BenchmarkTask(
                    id,
                    title,
                    category,
                    Set.copyOf(modules == null ? List.of() : modules),
                    prompt,
                    Duration.ofMinutes(timeoutMinutes == null ? DEFAULT_TIMEOUT_MINUTES : timeoutMinutes),
                    maxTurns == null ? DEFAULT_MAX_TURNS : maxTurns,
                    acceptanceSpec.toAcceptance(),
                    Set.copyOf(allowGuardrails == null ? List.of() : allowGuardrails),
                    directory);
        }
    }

    record AcceptanceSpec(Boolean agentTests, Integer minAgentTests, List<String> mutants, Boolean mainReadOnly,
            Boolean unitTestsOnly) {
        BenchmarkTask.Acceptance toAcceptance() {
            return new BenchmarkTask.Acceptance(
                    Boolean.TRUE.equals(agentTests),
                    minAgentTests == null ? 1 : minAgentTests,
                    mutants == null ? List.of() : mutants,
                    Boolean.TRUE.equals(mainReadOnly),
                    Boolean.TRUE.equals(unitTestsOnly));
        }
    }
}
