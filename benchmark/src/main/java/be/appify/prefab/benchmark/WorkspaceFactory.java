package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Creates the project an agent works in: the template, overlaid with the task's starting project and the context
 * setup, committed to a fresh git repository so the harness can see what the agent changed.
 *
 * <p>Workspaces live outside the Prefab repository, so the agent cannot stumble on hidden tests or reference
 * solutions.
 */
final class WorkspaceFactory {
    static final String STARTER_TAG = "starter";

    private final BenchmarkPaths paths;
    private final ProjectVersions versions;

    WorkspaceFactory(BenchmarkPaths paths, ProjectVersions versions) {
        this.paths = paths;
        this.versions = versions;
    }

    /**
     * Creates a workspace.
     *
     * @param task      the task
     * @param context   the documentation setup, applied after the starter
     * @param directory empty or non-existing directory to create the project in
     * @param log       file receiving the output of git
     * @return the git handle of the workspace
     */
    Git create(BenchmarkTask task, ContextSetup context, Path directory, Path log) {
        FileTrees.delete(directory);
        FileTrees.copy(paths.template(), directory);
        render(directory.resolve("pom.xml"), task);
        render(directory.resolve("src/main/resources/application.yml"), task);
        if (Files.isDirectory(task.starter())) {
            FileTrees.copy(task.starter(), directory);
        }
        context.applyTo(paths, directory);
        var git = new Git(directory, log);
        git.init();
        git.commitAll(STARTER_TAG);
        return git;
    }

    private void render(Path file, BenchmarkTask task) {
        var dependencies = task.modules().stream()
                .sorted()
                .map(PrefabModule::dependencyXml)
                .collect(Collectors.joining())
                .stripTrailing();
        var content = FileTrees.readString(file)
                .replace("@@SPRING_BOOT_VERSION@@", versions.springBoot())
                .replace("@@PREFAB_VERSION@@", versions.prefab())
                .replace("@@JAVA_VERSION@@", versions.java())
                .replace("@@ARTIFACT_ID@@", task.id())
                .replace("@@NAME@@", task.title())
                .replace("@@MODULE_DEPENDENCIES@@", dependencies);
        FileTrees.writeString(file, content);
    }
}
