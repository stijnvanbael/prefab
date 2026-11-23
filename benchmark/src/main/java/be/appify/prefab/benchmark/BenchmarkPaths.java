package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locations of the benchmark inside the Prefab repository.
 *
 * @param repository the Prefab repository root
 */
record BenchmarkPaths(Path repository) {

    /** Finds the repository root by walking up from a directory until {@code benchmark/tasks} exists. */
    static BenchmarkPaths discover(Path start) {
        for (var directory = start.toAbsolutePath().normalize(); directory != null; directory = directory.getParent()) {
            if (Files.isDirectory(directory.resolve("benchmark/tasks"))) {
                return new BenchmarkPaths(directory);
            }
        }
        throw new IllegalStateException("Cannot find benchmark/tasks above " + start.toAbsolutePath()
                + "; run from the Prefab repository or pass --repository");
    }

    Path benchmark() {
        return repository.resolve("benchmark");
    }

    Path tasks() {
        return benchmark().resolve("tasks");
    }

    Path template() {
        return benchmark().resolve("template");
    }

    Path contexts() {
        return benchmark().resolve("contexts");
    }

    Path results() {
        return benchmark().resolve("results");
    }

    Path acceptanceSupport() {
        return benchmark().resolve("acceptance-support");
    }

    Path agentPreamble() {
        return benchmark().resolve("agent-preamble.md");
    }
}
