package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Manual failure classifications, kept in {@code classifications.yml} next to a run's report and keyed by
 * {@code <task-id>#<attempt>}:
 *
 * <pre>
 * rest-crud-update-delete#1:
 *   category: API_MISUSE
 *   note: used @Update on a void method that returns nothing for a delete
 * </pre>
 *
 * Manual entries win over heuristics and triage, so a reviewed report stays reviewed when it is re-rendered.
 */
final class ClassificationOverrides {
    static final String FILE_NAME = "classifications.yml";
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    private ClassificationOverrides() {
    }

    static RunReport apply(RunReport report, Path runDirectory) {
        var file = runDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return report;
        }
        Map<String, Entry> entries = YAML.readValue(file.toFile(), new TypeReference<Map<String, Entry>>() {
        });
        var unknown = entries.keySet().stream()
                .filter(key -> report.results().stream().noneMatch(result -> result.key().equals(key)))
                .toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(FILE_NAME + " refers to unknown attempts " + unknown);
        }
        return report.withResults(report.results().stream().map(result -> {
            var entry = entries.get(result.key());
            if (entry == null || result.passed()) {
                return result;
            }
            var evidence = result.failure() == null ? List.<String>of() : result.failure().evidence();
            return result.withFailure(new TaskResult.Classification(entry.category(), "manual",
                    entry.note() == null ? "" : entry.note(), evidence));
        }).toList());
    }

    record Entry(FailureCategory category, String note) {
    }
}
