package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Everything a benchmark run produced, persisted as {@code report.json} next to {@code report.md}.
 *
 * @param runId         directory name of the run
 * @param startedAt     start time
 * @param agent         agent description
 * @param model         model under test
 * @param context       context setup name
 * @param prefabVersion Prefab version the workspaces built against
 * @param gitCommit     commit of the Prefab repository (with {@code -dirty} when it had local changes)
 * @param repeat        attempts per task
 * @param results       one entry per attempt
 */
record RunReport(
        String runId,
        Instant startedAt,
        String agent,
        String model,
        String context,
        String prefabVersion,
        String gitCommit,
        int repeat,
        List<TaskResult> results
) {
    static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    RunReport {
        results = List.copyOf(results);
    }

    static RunReport read(Path file) {
        return JSON.readValue(file.toFile(), RunReport.class);
    }

    void write(Path file) {
        FileTrees.writeString(file, JSON.writeValueAsString(this));
    }

    RunReport withResults(List<TaskResult> results) {
        return new RunReport(runId, startedAt, agent, model, context, prefabVersion, gitCommit, repeat, results);
    }

    long passedCount() {
        return results.stream().filter(TaskResult::passed).count();
    }

    Statistics.Interval passRate() {
        return Statistics.wilson(passedCount(), results.size());
    }
}
