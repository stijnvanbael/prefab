package be.appify.prefab.benchmark;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Renders a {@link RunReport} as Markdown, and compares several runs side by side. */
final class MarkdownReport {

    private MarkdownReport() {
    }

    static String render(RunReport report) {
        var md = new StringBuilder();
        var results = report.results();
        md.append("# Prefab benchmark: ").append(report.model()).append(" with context `")
                .append(report.context()).append("`\n\n");
        md.append("| | |\n|---|---|\n");
        row(md, "Run", report.runId());
        row(md, "Agent", report.agent());
        row(md, "Context", report.context());
        row(md, "Prefab", report.prefabVersion() + " @ " + report.gitCommit());
        row(md, "Started", report.startedAt().toString());
        row(md, "Attempts", "%d (%d tasks x %d)".formatted(results.size(),
                results.stream().map(TaskResult::taskId).distinct().count(), report.repeat()));
        row(md, "**Pass rate**", "**%d/%d = %s**".formatted(report.passedCount(), results.size(), report.passRate().format()));
        row(md, "Tasks passed in every attempt", Long.toString(stableTasks(results)));
        row(md, "Cost", "$%.2f".formatted(results.stream().mapToDouble(result -> result.agent().costUsd()).sum()));
        row(md, "Tokens (input incl. cache / output)", "%,d / %,d".formatted(
                results.stream().mapToLong(result -> result.agent().usage().totalInput()).sum(),
                results.stream().mapToLong(result -> result.agent().usage().output()).sum()));
        row(md, "Median turns / builds / agent time", "%.0f / %.0f / %s".formatted(
                Statistics.median(results.stream().map(result -> result.agent().turns()).toList()),
                Statistics.median(results.stream().map(result -> result.agent().buildAttempts()).toList()),
                Statistics.formatDuration(Duration.ofSeconds((long) Statistics.median(
                        results.stream().map(result -> result.agent().duration().toSeconds()).toList())))));
        var notRun = results.stream().filter(result -> result.agent().status() == Agent.Status.RATE_LIMITED
                || result.agent().status() == Agent.Status.SKIPPED).count();
        if (notRun > 0) {
            row(md, "**Rate limited or skipped**", notRun + " attempts did not run; the pass rate is not meaningful");
        }
        if (results.stream().anyMatch(result -> result.agent().contaminated())) {
            row(md, "**Contaminated attempts**", results.stream().filter(result -> result.agent().contaminated())
                    .map(TaskResult::key).collect(Collectors.joining(", ")));
        }

        md.append("\n## Failure categories\n\n| Category | Attempts |\n|---|---|\n");
        var categories = results.stream().filter(result -> result.failure() != null)
                .collect(Collectors.groupingBy(result -> result.failure().category(), TreeMap::new, Collectors.counting()));
        Arrays.stream(FailureCategory.values()).forEach(category ->
                md.append("| ").append(category).append(" | ").append(categories.getOrDefault(category, 0L)).append(" |\n"));

        md.append("\n## By task category\n\n| Category | Passed |\n|---|---|\n");
        results.stream().collect(Collectors.groupingBy(TaskResult::category, TreeMap::new, Collectors.toList()))
                .forEach((category, attempts) -> md.append("| ").append(category).append(" | ")
                        .append(attempts.stream().filter(TaskResult::passed).count()).append('/')
                        .append(attempts.size()).append(" |\n"));

        md.append("\n## Attempts\n\n")
                .append("| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (var result : results) {
            var agent = result.agent();
            md.append("| ").append(result.taskId())
                    .append(" | ").append(result.attempt())
                    .append(" | ").append(result.passed() ? "PASS" : "FAIL")
                    .append(" | ").append(checkSummary(result))
                    .append(" | ").append(agent.turns())
                    .append(" | ").append(agent.buildAttempts())
                    .append(" | ").append("%,d / %,d".formatted(agent.usage().totalInput(), agent.usage().output()))
                    .append(" | ").append("$%.2f".formatted(agent.costUsd()))
                    .append(" | ").append(Statistics.formatDuration(agent.duration()))
                    .append(" | ").append(result.failure() == null ? "" : result.failure().category())
                    .append(" |\n");
        }

        var failures = results.stream().filter(result -> !result.passed()).toList();
        if (!failures.isEmpty()) {
            md.append("\n## Failures\n");
            for (var result : failures) {
                md.append("\n### ").append(result.key()).append("\n\n");
                var failure = result.failure();
                if (failure != null) {
                    md.append("**").append(failure.category()).append("** (").append(failure.source()).append("): ")
                            .append(escape(failure.rationale())).append("\n\n");
                    failure.evidence().forEach(line -> md.append("- `").append(code(line)).append("`\n"));
                }
                md.append("\nLogs: `").append(result.logs()).append("`\n");
            }
        }
        return md.toString();
    }

    /** Renders a comparison of several runs: one column per run, one row per task. */
    static String compare(List<RunReport> reports) {
        var md = new StringBuilder("# Prefab benchmark comparison\n\n| | ");
        md.append(reports.stream().map(MarkdownReport::label).collect(Collectors.joining(" | "))).append(" |\n|---|");
        md.append("---|".repeat(reports.size())).append('\n');
        md.append("| **Pass rate** | ").append(reports.stream()
                .map(report -> "**%d/%d** %s".formatted(report.passedCount(), report.results().size(), report.passRate().format()))
                .collect(Collectors.joining(" | "))).append(" |\n");
        md.append("| Cost | ").append(reports.stream()
                .map(report -> "$%.2f".formatted(report.results().stream().mapToDouble(r -> r.agent().costUsd()).sum()))
                .collect(Collectors.joining(" | "))).append(" |\n");
        md.append("| Median turns | ").append(reports.stream()
                .map(report -> "%.0f".formatted(Statistics.median(report.results().stream().map(r -> r.agent().turns()).toList())))
                .collect(Collectors.joining(" | "))).append(" |\n");
        for (var category : FailureCategory.values()) {
            md.append("| ").append(category).append(" | ").append(reports.stream()
                    .map(report -> Long.toString(report.results().stream()
                            .filter(r -> r.failure() != null && r.failure().category() == category).count()))
                    .collect(Collectors.joining(" | "))).append(" |\n");
        }
        var byTask = new TreeMap<String, Map<String, List<TaskResult>>>();
        for (var report : reports) {
            report.results().forEach(result -> byTask
                    .computeIfAbsent(result.taskId(), id -> new LinkedHashMap<>())
                    .computeIfAbsent(report.runId(), id -> new java.util.ArrayList<>())
                    .add(result));
        }
        byTask.forEach((task, runs) -> {
            md.append("| ").append(task).append(" | ");
            md.append(reports.stream().map(report -> cell(runs.get(report.runId()))).collect(Collectors.joining(" | ")));
            md.append(" |\n");
        });
        return md.toString();
    }

    private static String cell(List<TaskResult> attempts) {
        if (attempts == null) {
            return "-";
        }
        var passed = attempts.stream().filter(TaskResult::passed).count();
        var categories = attempts.stream().filter(result -> result.failure() != null)
                .map(result -> abbreviation(result.failure().category()))
                .distinct()
                .collect(Collectors.joining(","));
        var status = passed == attempts.size() ? "PASS" : passed == 0 ? "FAIL" : "FLAKY";
        return "%s %d/%d%s".formatted(status, passed, attempts.size(), categories.isEmpty() ? "" : " " + categories);
    }

    private static String abbreviation(FailureCategory category) {
        return switch (category) {
            case DOC_GAP -> "doc";
            case MISLEADING_DIAGNOSTIC -> "diag";
            case API_MISUSE -> "api";
            case PLAIN_SPRING_FALLBACK -> "spring";
            case TEST_OR_INFRASTRUCTURE -> "infra";
        };
    }

    private static String label(RunReport report) {
        return report.model() + " / " + report.context() + "<br>`" + report.runId() + "`";
    }

    private static long stableTasks(List<TaskResult> results) {
        return results.stream().collect(Collectors.groupingBy(TaskResult::taskId)).values().stream()
                .filter(attempts -> attempts.stream().allMatch(TaskResult::passed))
                .count();
    }

    private static String checkSummary(TaskResult result) {
        return result.acceptance().checks().stream()
                .map(check -> (check.passed() ? "+" : "-") + check.name())
                .collect(Collectors.joining(" "));
    }

    private static void row(StringBuilder md, String key, String value) {
        md.append("| ").append(key).append(" | ").append(value).append(" |\n");
    }

    private static String escape(String text) {
        return text.replace("\n", " ").replace("|", "\\|");
    }

    private static String code(String text) {
        var single = text.replace("\n", " ").replace("`", "'");
        return single.length() > 400 ? single.substring(0, 400) + "..." : single;
    }
}
