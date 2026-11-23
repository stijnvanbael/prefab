package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Optional LLM triage of failed attempts: a (stronger) model reads the task, the agent's diff and the failure evidence
 * and picks a {@link FailureCategory}. It replaces the heuristic classification but not a manual one.
 *
 * @param model      model id or alias
 * @param executable the Claude Code CLI
 */
record Triage(String model, String executable) {
    private static final int MAX_DIFF_CHARS = 40_000;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[^{}]*\"category\"[^{}]*}");

    TaskResult classify(BenchmarkTask task, TaskResult result, Path logs) {
        var prompt = prompt(task, result, logs);
        var output = logs.resolve("triage.json");
        var options = new ProcessRunner.ProcessOptions(logs, java.util.Map.of(),
                List.of("CLAUDECODE", "CLAUDE_CODE_ENTRYPOINT"), output, logs.resolve("triage-stderr.log"),
                Duration.ofMinutes(5));
        ProcessRunner.run(List.of(executable, "-p", prompt, "--model", model, "--output-format", "json",
                "--max-turns", "1", "--setting-sources", "project", "--no-session-persistence",
                "--disallowedTools", "Bash,Edit,Write,Read,Glob,Grep,WebFetch,WebSearch"), options);
        try {
            var answer = JSON.readTree(FileTrees.readStringIfExists(output)).path("result").asString("");
            var matcher = JSON_OBJECT.matcher(answer);
            if (!matcher.find()) {
                return result;
            }
            var verdict = JSON.readTree(matcher.group());
            var category = FailureCategory.valueOf(verdict.path("category").asString(""));
            var previous = result.failure();
            return result.withFailure(new TaskResult.Classification(category, "triage:" + model,
                    verdict.path("rationale").asString(""), previous == null ? List.of() : previous.evidence()));
        } catch (JacksonException | IllegalArgumentException e) {
            return result;
        }
    }

    private static String prompt(BenchmarkTask task, TaskResult result, Path logs) {
        var diff = FileTrees.readStringIfExists(logs.resolve("agent.diff"));
        if (diff.length() > MAX_DIFF_CHARS) {
            diff = diff.substring(0, MAX_DIFF_CHARS) + "\n... (truncated)";
        }
        var evidence = result.failure() == null ? "" : String.join("\n", result.failure().evidence());
        var categories = Arrays.stream(FailureCategory.values()).map(Enum::name).collect(Collectors.joining(", "));
        return """
                You are triaging a failed attempt of an AI coding agent in a benchmark for Prefab, a Java framework \
                that generates Spring Boot controllers, services, repositories, migrations and test clients from \
                annotated domain records. Classify the root cause of the failure into exactly one category:

                - DOC_GAP: the agent did not know how to achieve the requirement with Prefab (documentation is missing \
                or hard to find).
                - MISLEADING_DIAGNOSTIC: a Prefab compiler error, warning or runtime message misled the agent or did \
                not help it fix the problem.
                - API_MISUSE: the agent used a Prefab annotation or type in an unsupported way, or named things \
                differently from what the task asked.
                - PLAIN_SPRING_FALLBACK: the agent wrote plain Spring/JPA code (controllers, entities, repositories, \
                JdbcTemplate, SQL migrations) instead of letting Prefab generate it.
                - TEST_OR_INFRASTRUCTURE: the benchmark task, its hidden tests or the environment are at fault, not \
                the agent.

                ## Task given to the agent
                %s

                ## Failure evidence
                %s

                ## Agent's final message
                %s

                ## Agent's diff
                ```diff
                %s
                ```

                Answer with a single JSON object and nothing else:
                {"category": "<one of %s>", "rationale": "<one or two sentences>"}
                """.formatted(task.prompt(), evidence, result.agent().message(), diff, categories);
    }
}
