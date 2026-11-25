package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskCatalogTest {
    private static final BenchmarkPaths PATHS = BenchmarkPaths.discover(Path.of("."));
    private final TaskCatalog catalog = new TaskCatalog(PATHS.tasks());

    @Test
    void benchmarkHasAtLeastThirtyValidTasks() {
        assertThat(catalog.all()).hasSizeGreaterThanOrEqualTo(30);
    }

    @Test
    void tiersHaveEnoughTasksToMeasureADifference() {
        var tiers = catalog.all().stream().collect(Collectors.groupingBy(BenchmarkTask::tier, Collectors.counting()));

        assertThat(tiers.get(Tier.QUICK)).isGreaterThanOrEqualTo(32);
        assertThat(tiers.get(Tier.FEATURE)).isGreaterThanOrEqualTo(8);
        assertThat(tiers.get(Tier.BROWNFIELD)).isGreaterThanOrEqualTo(6);
    }

    @Test
    void brownfieldTasksStartFromABaseApplicationAndQuickTasksDoNot() {
        assertThat(catalog.all()).allSatisfy(task -> {
            assertThat(task.base().isPresent()).as(task.id()).isEqualTo(task.tier() == Tier.BROWNFIELD);
            task.base().ifPresent(app -> assertThat(task.modules()).as(task.id()).containsAll(app.modules()));
        });
    }

    @Test
    void selectMatchesTiers() {
        assertThat(catalog.select(List.of("brownfield"))).isNotEmpty()
                .allMatch(task -> task.tier() == Tier.BROWNFIELD);
        assertThat(catalog.select(List.of("quick"))).hasSize((int) catalog.all().stream()
                .filter(task -> task.tier() == Tier.QUICK).count());
    }

    @Test
    void loadRejectsBrownfieldTasksWithoutBase(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var task = directory.resolve("tasks/no-base");
        Files.createDirectories(task.resolve("hidden"));
        Files.createDirectories(task.resolve("reference"));
        Files.writeString(task.resolve("task.yml"), "title: No base\ncategory: brownfield-bugfix\ntier: brownfield\n");
        Files.writeString(task.resolve("prompt.md"), "Fix it");

        assertThatThrownBy(() -> TaskCatalog.load(task)).hasMessageContaining("base application");
    }

    @Test
    void loadRejectsUnknownBaseApplications(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var task = directory.resolve("tasks/unknown-base");
        Files.createDirectories(task.resolve("hidden"));
        Files.createDirectories(task.resolve("reference"));
        Files.writeString(task.resolve("task.yml"),
                "title: Unknown\ncategory: brownfield-bugfix\ntier: brownfield\nbase: nowhere\n");
        Files.writeString(task.resolve("prompt.md"), "Fix it");

        assertThatThrownBy(() -> TaskCatalog.load(task)).hasMessageContaining("nowhere");
    }

    @Test
    void tasksCoverTheMainPrefabUseCases() {
        var categories = catalog.all().stream().map(BenchmarkTask::category).collect(Collectors.toSet());

        assertThat(categories).contains("rest-crud", "lists", "events-publishing", "events-handling",
                "repository-mixins", "security", "multi-tenancy", "binary", "avro", "testing");
    }

    @Test
    void everyTaskHasAPromptAndAcceptanceCriteria() {
        assertThat(catalog.all()).allSatisfy(task -> {
            assertThat(task.prompt()).as(task.id()).isNotBlank();
            assertThat(task.hasHiddenTests() || task.acceptance().agentTests()).as(task.id()).isTrue();
        });
    }

    @Test
    void hiddenTestsAreNotPartOfTheStartingProject() {
        assertThat(catalog.all()).allSatisfy(task -> AcceptanceRunner.hiddenTestClasses(task).forEach(testClass -> {
            var file = testClass.replace('.', '/') + ".java";
            assertThat(task.starter().resolve("src/test/java").resolve(file)).as(task.id()).doesNotExist();
            task.base().ifPresent(app ->
                    assertThat(app.project().resolve("src/test/java").resolve(file)).as(task.id()).doesNotExist());
        }));
    }

    @Test
    void selectMatchesIdsAndCategoriesWithGlobs() {
        var selected = catalog.select(List.of("rest-create-and-get", "avro"));

        assertThat(selected).extracting(BenchmarkTask::id)
                .contains("rest-create-and-get")
                .allMatch(id -> id.equals("rest-create-and-get") || id.startsWith("avro-"));
        assertThat(catalog.select(List.of("events-*"))).isNotEmpty()
                .allMatch(task -> task.id().startsWith("events-"));
    }

    @Test
    void selectRejectsPatternsWithoutMatches() {
        assertThatThrownBy(() -> catalog.select(List.of("no-such-task")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-such-task");
    }

    @Test
    void loadRejectsTasksWithoutReference(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var task = directory.resolve("broken");
        Files.createDirectories(task.resolve("hidden"));
        Files.writeString(task.resolve("task.yml"), "title: Broken\ncategory: rest-crud\n");
        Files.writeString(task.resolve("prompt.md"), "Do something");

        assertThatThrownBy(() -> TaskCatalog.load(task)).hasMessageContaining("reference");
    }

    @Test
    void loadRejectsUnknownProperties(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var task = directory.resolve("typo");
        Files.createDirectories(task.resolve("hidden"));
        Files.createDirectories(task.resolve("reference"));
        Files.writeString(task.resolve("task.yml"), "title: Typo\ncategory: rest-crud\nmodule: [postgres]\n");
        Files.writeString(task.resolve("prompt.md"), "Do something");

        assertThatThrownBy(() -> TaskCatalog.load(task)).hasMessageContaining("Invalid task typo");
    }
}
