package be.appify.prefab.benchmark;

import java.nio.file.Files;

/**
 * Applies the task's reference solution ({@code reference/} overlays the workspace; an optional
 * {@code reference/.delete} lists workspace-relative files to remove). Used by {@code validate}: the acceptance checks
 * must pass on the reference, otherwise the task or its hidden tests are broken.
 */
record ReferenceAgent() implements Agent {
    static final String DELETE_LIST = ".delete";

    @Override
    public String name() {
        return "reference";
    }

    @Override
    public String model() {
        return "reference";
    }

    @Override
    public AgentOutcome run(AgentRun run) {
        var reference = run.task().reference();
        var deleteList = reference.resolve(DELETE_LIST);
        if (Files.isRegularFile(deleteList)) {
            FileTrees.readString(deleteList).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(path -> FileTrees.delete(run.workspace().resolve(path)));
        }
        for (var file : FileTrees.files(reference)) {
            if (!file.equals(deleteList)) {
                FileTrees.copy(file, run.workspace().resolve(reference.relativize(file).toString()));
            }
        }
        return AgentOutcome.instant("applied the reference solution");
    }
}
