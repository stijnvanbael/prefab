package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceFactoryTest {
    private static final BenchmarkPaths PATHS = BenchmarkPaths.discover(Path.of("."));

    @TempDir
    Path directory;

    @Test
    void createsACommittedProjectFromTemplateStarterAndContext() throws Exception {
        var task = TaskCatalog.load(PATHS.tasks().resolve("rest-update-and-delete"));
        var context = ContextSetup.load(PATHS.contexts(), "docs-folder");
        var workspace = directory.resolve("project");

        var git = new WorkspaceFactory(PATHS, new ProjectVersions("9.9.9", "4.1.2", "21"))
                .create(task, context, workspace, directory.resolve("git.log"));

        var pom = Files.readString(workspace.resolve("pom.xml"));
        assertThat(pom).doesNotContain("@@")
                .contains("<prefab.version>9.9.9</prefab.version>")
                .contains("<artifactId>prefab-postgres</artifactId>");
        assertThat(workspace.resolve("src/main/java/com/example/catalog/Book.java")).exists();
        assertThat(workspace.resolve("src/main/java/com/example/Application.java")).exists();
        assertThat(workspace.resolve("docs/prefab/annotation-reference.md")).exists();
        assertThat(Files.readString(workspace.resolve("CLAUDE.md"))).contains("docs/prefab/");
        assertThat(git.changesSince(WorkspaceFactory.STARTER_TAG)).isEmpty();
    }

    @Test
    void brownfieldWorkspacesStartFromTheBaseApplicationWithTheStarterOnTop() throws Exception {
        var task = TaskCatalog.load(PATHS.tasks().resolve("brownfield-bug-deactivation"));
        var workspace = directory.resolve("project");

        var git = new WorkspaceFactory(PATHS, new ProjectVersions("9.9.9", "4.1.2", "21"))
                .create(task, ContextSetup.load(PATHS.contexts(), "none"), workspace, directory.resolve("git.log"));

        assertThat(Files.readString(workspace.resolve("pom.xml")))
                .contains("<artifactId>prefab-postgres</artifactId>")
                .contains("<artifactId>prefab-kafka</artifactId>")
                .contains("<artifactId>prefab-security</artifactId>");
        assertThat(workspace.resolve("src/main/java/com/example/shop/catalog/Product.java")).exists();
        assertThat(workspace.resolve("src/test/java/com/example/shop/support/Shop.java")).exists();
        assertThat(workspace.resolve("src/main/resources/db/migration/V1__generated.sql")).exists();
        assertThat(Files.readString(workspace.resolve("src/main/java/com/example/shop/ordering/Order.java")))
                .isEqualTo(Files.readString(task.starter().resolve("src/main/java/com/example/shop/ordering/Order.java")));
        assertThat(Files.readString(workspace.resolve("src/main/resources/application.yml"))).doesNotContain("@@");
        assertThat(git.changesSince(WorkspaceFactory.STARTER_TAG)).isEmpty();
    }

    @Test
    void contextsAssembleTheirInstructions() throws Exception {
        var none = ContextSetup.load(PATHS.contexts(), "none");
        var fullDocs = ContextSetup.load(PATHS.contexts(), "full-docs");
        var recipes = ContextSetup.load(PATHS.contexts(), "recipes");

        none.applyTo(PATHS, directory.resolve("none"));
        fullDocs.applyTo(PATHS, directory.resolve("full"));
        recipes.applyTo(PATHS, directory.resolve("recipes"));

        assertThat(directory.resolve("none/CLAUDE.md")).doesNotExist();
        assertThat(Files.readString(directory.resolve("full/CLAUDE.md")))
                .contains("# Annotation Reference")
                .contains("# Feature Guides");
        assertThat(Files.readString(directory.resolve("recipes/CLAUDE.md")))
                .contains("# Prefab Task Recipes");
        assertThat(directory.resolve("recipes/docs/prefab/recipes/aggregate-rest-crud.md")).exists();
        assertThat(directory.resolve(
                "recipes/examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/Product.java"))
                .exists();
    }

    @Test
    void readsVersionsFromTheRootPom() {
        var versions = ProjectVersions.read(PATHS.repository());

        assertThat(versions.prefab()).isNotBlank();
        assertThat(versions.springBoot()).matches("\\d+\\.\\d+\\.\\d+");
        assertThat(versions.java()).matches("\\d+");
    }
}
