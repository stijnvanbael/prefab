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
    void contextsAssembleTheirInstructions() throws Exception {
        var none = ContextSetup.load(PATHS.contexts(), "none");
        var fullDocs = ContextSetup.load(PATHS.contexts(), "full-docs");

        none.applyTo(PATHS, directory.resolve("none"));
        fullDocs.applyTo(PATHS, directory.resolve("full"));

        assertThat(directory.resolve("none/CLAUDE.md")).doesNotExist();
        assertThat(Files.readString(directory.resolve("full/CLAUDE.md")))
                .contains("# Annotation Reference")
                .contains("# Feature Guides");
    }

    @Test
    void readsVersionsFromTheRootPom() {
        var versions = ProjectVersions.read(PATHS.repository());

        assertThat(versions.prefab()).isNotBlank();
        assertThat(versions.springBoot()).matches("\\d+\\.\\d+\\.\\d+");
        assertThat(versions.java()).matches("\\d+");
    }
}
