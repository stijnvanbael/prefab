package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * A documentation setup the agent works with, defined in {@code benchmark/contexts/<name>/context.yml}.
 *
 * <p>A context assembles an instructions file (CLAUDE.md by default) from files in the context directory and in the
 * Prefab repository, and copies further repository files into the workspace for the agent to read on demand.
 * Comparing contexts is the point of the benchmark: does curated guidance beat the full docs, or no docs at all?
 *
 * @param name             directory name
 * @param description      what the setup represents
 * @param instructionsFile workspace-relative file the instructions are written to
 * @param instructions     sources concatenated into the instructions file, in order
 * @param copy             repository files or directories copied into the workspace
 */
record ContextSetup(String name, String description, String instructionsFile, List<Source> instructions, List<Copy> copy) {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    ContextSetup {
        instructionsFile = instructionsFile == null ? "CLAUDE.md" : instructionsFile;
        instructions = instructions == null ? List.of() : List.copyOf(instructions);
        copy = copy == null ? List.of() : List.copyOf(copy);
    }

    static ContextSetup load(Path contextsDirectory, String name) {
        var file = contextsDirectory.resolve(name).resolve("context.yml");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Unknown context '" + name + "': " + file + " does not exist");
        }
        var spec = YAML.readValue(file.toFile(), ContextSetup.class);
        return new ContextSetup(name, spec.description(), spec.instructionsFile(), spec.instructions(), spec.copy());
    }

    static List<String> available(Path contextsDirectory) {
        try (Stream<Path> directories = Files.list(contextsDirectory)) {
            return directories.filter(directory -> Files.isRegularFile(directory.resolve("context.yml")))
                    .map(directory -> directory.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes the instructions file and copies the context files into a workspace.
     *
     * @param paths     benchmark locations
     * @param workspace the project root
     */
    void applyTo(BenchmarkPaths paths, Path workspace) {
        var contextDirectory = paths.contexts().resolve(name);
        var text = new StringBuilder();
        for (var source : instructions) {
            for (var file : source.resolve(contextDirectory, paths.repository())) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append(read(file).strip());
            }
        }
        try {
            if (!text.isEmpty()) {
                var target = workspace.resolve(instructionsFile);
                Files.createDirectories(target.getParent());
                Files.writeString(target, text.append('\n').toString());
            }
            for (var entry : copy) {
                FileTrees.copy(paths.repository().resolve(entry.from()), workspace.resolve(entry.to()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot apply context " + name, e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read context source " + file, e);
        }
    }

    /**
     * One source of instructions: either a file in the context directory or a glob in the Prefab repository.
     *
     * @param context file relative to the context directory
     * @param repo    file or glob relative to the repository root; glob matches are concatenated in name order
     */
    record Source(String context, String repo) {
        List<Path> resolve(Path contextDirectory, Path repository) {
            if (context != null) {
                return List.of(existing(contextDirectory.resolve(context)));
            }
            if (repo == null) {
                throw new IllegalArgumentException("An instructions source needs 'context' or 'repo'");
            }
            if (!repo.contains("*")) {
                return List.of(existing(repository.resolve(repo)));
            }
            var matcher = FileSystems.getDefault().getPathMatcher("glob:" + repo);
            try (Stream<Path> files = Files.walk(repository, 4)) {
                var matches = files.filter(file -> matcher.matches(repository.relativize(file)))
                        .filter(Files::isRegularFile)
                        .sorted()
                        .toList();
                if (matches.isEmpty()) {
                    throw new IllegalArgumentException("Context glob '" + repo + "' matches no files");
                }
                return matches;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static Path existing(Path file) {
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("Context source " + file + " does not exist");
            }
            return file;
        }
    }

    /**
     * A repository file or directory copied into the workspace.
     *
     * @param from path relative to the repository root
     * @param to   path relative to the workspace root
     */
    record Copy(String from, String to) {
    }
}
