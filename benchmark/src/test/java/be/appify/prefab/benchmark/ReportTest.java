package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

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
                .contains("| rest-a | quick | 1 | PASS |")
                .contains("| rest-b | quick | 1 | FAIL |")
                .contains("| DOC_GAP | 1 |")
                .contains("### rest-b#1")
                .contains("$0.50");
    }

    @Test
    @ResourceLock(Resources.LOCALE)
    void markdownFormattingIsIndependentOfDefaultLocale() {
        var originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("nl-BE"));

            var markdown = MarkdownReport.render(report);

            assertThat(markdown)
                    .contains("| Cost | $0.50 |")
                    .contains("| Tokens (input incl. cache / output) | 2,300 / 40 |")
                    .contains("1,150 / 20")
                    .contains("50% (95% CI 9-91%)");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void markdownReportsPassRatesAndMetricsPerTier() {
        var tiered = Fixtures.report("run-3", List.of(
                Fixtures.result("rest-a", 1, true, null),
                Fixtures.result("feature-x", Tier.FEATURE, 1, false, FailureCategory.DOC_GAP),
                Fixtures.result("brownfield-y", Tier.BROWNFIELD, 1, true, null)));

        var markdown = MarkdownReport.render(tiered);

        assertThat(markdown)
                .contains("## By tier")
                .contains("| quick | 1/1 |")
                .contains("| feature | 0/1 |")
                .contains("| brownfield | 1/1 |")
                .contains("| Median time to first green build | 40s (3/3 attempts) |")
                .contains("| feature-x | feature | 1 | FAIL |");
    }

    @Test
    void countsOverriddenGeneratedClassesPerAttempt() {
        var violation = new Guardrails.Violation("generated-class-override", Guardrails.Severity.FAIL, "replaces",
                "src/main/java/com/example/BookController.java", 0);
        var controller = new Guardrails.Violation("hand-written-controller", Guardrails.Severity.FAIL, "controller",
                "src/main/java/com/example/BookController.java", 3);
        var migration = new Guardrails.Violation("hand-written-migration", Guardrails.Severity.WARN, "migration",
                "src/main/resources/db/migration/V2__x.sql", 0);
        var result = Fixtures.result("rest-a", 1, false, FailureCategory.PLAIN_SPRING_FALLBACK);
        var acceptance = result.acceptance();
        var withViolations = new TaskResult(result.taskId(), result.category(), result.tier(), 1, false,
                result.agent(), new AcceptanceRunner.AcceptanceResult(acceptance.checks(),
                List.of(violation, controller, migration), acceptance.changes(), acceptance.hiddenTests(),
                acceptance.buildErrors(), false), result.acceptanceDuration(), result.failure(), result.logs());

        assertThat(withViolations.overrides()).isEqualTo(1);
        assertThat(MarkdownReport.render(Fixtures.report("run-4", List.of(withViolations))))
                .contains("| Generated classes overridden or hand-written | 1 (in 1 attempts) |");
    }

    @Test
    void reportsWrittenBeforeTiersReadAsQuick() throws Exception {
        var file = directory.resolve("report.json");
        report.write(file);
        Files.writeString(file, Files.readString(file).replace("\"tier\" : \"quick\",", "")
                .replaceAll(",\\s*\"firstGreenBuild\" : [^,}]+", ""));

        var read = RunReport.read(file);

        assertThat(read.results()).allSatisfy(result -> {
            assertThat(result.tier()).isEqualTo(Tier.QUICK);
            assertThat(result.agent().firstGreenBuild()).isEmpty();
        });
    }

    @Test
    void compareShowsOneColumnPerRun() {
        var other = Fixtures.report("run-2", List.of(
                Fixtures.result("rest-a", 1, false, FailureCategory.PLAIN_SPRING_FALLBACK),
                Fixtures.result("rest-b", 1, true, null)));

        var markdown = MarkdownReport.compare(List.of(report, other));

        assertThat(markdown)
                .contains("| rest-a | PASS 1/1 | FAIL 0/1 spring |")
                .contains("| rest-b | FAIL 0/1 doc | PASS 1/1 |")
                .contains("| Pass rate, quick tier | 1/2 50% (95% CI 9-91%) | 1/2 50% (95% CI 9-91%) |");
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
