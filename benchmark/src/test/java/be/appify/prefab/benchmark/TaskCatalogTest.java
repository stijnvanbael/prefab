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
