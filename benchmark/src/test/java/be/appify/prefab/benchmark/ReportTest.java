package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportTest {
    @TempDir
    Path directory;

    private final RunReport report = Fixtures.report("run-1", List.of(
            Fixtures.result("rest-a", 1, true, null),
            Fixtures.result("rest-b", 1, false, FailureCategory.DOC_GAP)));

    @Test
    void jsonRoundTripPreservesEverything() {
        var file = directory.resolve("report.json");

        report.write(file);

        assertThat(RunReport.read(file)).isEqualTo(report);
    }

    @Test
    void markdownSummarisesPassRateMetricsAndFailures() {
        var markdown = MarkdownReport.render(report);

        assertThat(markdown)
                .contains("**1/2 = 50% (95% CI 9-91%)**")
                .contains("| rest-a | 1 | PASS |")
                .contains("| rest-b | 1 | FAIL |")
                .contains("| DOC_GAP | 1 |")
                .contains("### rest-b#1")
                .contains("$0.50");
    }

    @Test
    void compareShowsOneColumnPerRun() {
        var other = Fixtures.report("run-2", List.of(
                Fixtures.result("rest-a", 1, false, FailureCategory.PLAIN_SPRING_FALLBACK),
                Fixtures.result("rest-b", 1, true, null)));

        var markdown = MarkdownReport.compare(List.of(report, other));

        assertThat(markdown)
                .contains("| rest-a | PASS 1/1 | FAIL 0/1 spring |")
                .contains("| rest-b | FAIL 0/1 doc | PASS 1/1 |");
    }

    @Test
    void manualClassificationsOverrideHeuristics() throws Exception {
        Files.writeString(directory.resolve(ClassificationOverrides.FILE_NAME), """
                rest-b#1:
                  category: API_MISUSE
                  note: named the field differently
                """);

        var classified = ClassificationOverrides.apply(report, directory);

        assertThat(classified.results().get(1).failure())
                .satisfies(failure -> {
                    assertThat(failure.category()).isEqualTo(FailureCategory.API_MISUSE);
                    assertThat(failure.source()).isEqualTo("manual");
                    assertThat(failure.rationale()).isEqualTo("named the field differently");
                });
        assertThat(classified.results().getFirst().failure()).isNull();
    }

    @Test
    void manualClassificationsMustReferToExistingAttempts() throws Exception {
        Files.writeString(directory.resolve(ClassificationOverrides.FILE_NAME), "rest-z#1:\n  category: DOC_GAP\n");

        assertThatThrownBy(() -> ClassificationOverrides.apply(report, directory))
                .hasMessageContaining("rest-z#1");
    }
}
