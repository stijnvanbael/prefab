package be.appify.prefab.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.TypeElement;

/**
 * Module-local state that survives between compiler invocations, so an IDE that recompiles only some sources still
 * gets complete aggregated output. Tracks the module root, the sources javac received, the owners of generated test
 * sources and pending input indexes; nothing is persisted unless processing finishes without errors.
 */
final class IncrementalBuildState {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Set<String> compiled = new TreeSet<>();
    private final Map<String, Set<String>> written = new TreeMap<>();
    private final Map<String, String> sourceByType = new TreeMap<>();
    private final Map<Path, Object> pendingIndexes = new TreeMap<>();
    private final RepositoryMixins mixins = new RepositoryMixins();
    private Optional<Path> root = Optional.empty();
    private String scope = "main";

    void observe(PrefabContext context) {
        for (var element : context.roundEnvironment().getRootElements()) {
            if (!(element instanceof TypeElement type)) {
                continue;
            }
            TestJavaFileWriter.sourcePath(context, type).ifPresent(source -> {
                if (root.isEmpty()) {
                    root = TestJavaFileWriter.rootPathFromSourcePath(source.toString()).map(Path::of);
                    if (root.isPresent()) {
                        var relative = root.get().relativize(source);
                        scope = relative.getName(1).toString().contains("test") ? "test" : "main";
                    }
                }
                root.filter(source::startsWith).ifPresent(base -> {
                    var path = relative(base, source);
                    compiled.add(path);
                    sourceByType.put(type.getQualifiedName().toString(), path);
                });
            });
        }
        mixins.observe(context);
    }

    RepositoryMixins mixins() {
        return mixins;
    }

    Optional<String> root() {
        return root.map(Path::toString);
    }

    Optional<Path> indexPath(String name) {
        return root.map(base -> base.resolve("target/prefab/incremental-v1").resolve(scope).resolve(name + ".json"));
    }

    Optional<String> sourceOf(String owner) {
        return Optional.ofNullable(sourceByType.get(owner));
    }

    boolean replaced(String path) {
        return compiled.contains(path) || root.map(base -> !Files.isRegularFile(base.resolve(path))).orElse(false);
    }

    void saveOnSuccess(Path index, Object value) {
        pendingIndexes.put(index, value);
    }

    void record(String file, List<TypeElement> owners, PrefabContext context) {
        if (root.isEmpty()) {
            return;
        }
        var sources = written.computeIfAbsent(file, ignored -> new TreeSet<>());
        owners.stream().map(owner -> TestJavaFileWriter.sourcePath(context, owner))
                .flatMap(Optional::stream).filter(path -> path.startsWith(root.get()))
                .map(path -> relative(root.get(), path)).forEach(sources::add);
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    void finish() {
        root.ifPresent(base -> {
            var inventory = base.resolve("target/prefab/test-sources-" + scope + ".json");
            try {
                Map<String, Set<String>> previous = Files.exists(inventory) ? JSON.readValue(inventory.toFile(),
                        new TypeReference<TreeMap<String, Set<String>>>() {}) : new TreeMap<>();
                var next = new TreeMap<>(written);
                for (var entry : previous.entrySet()) {
                    var remaining = new TreeSet<>(entry.getValue());
                    remaining.removeIf(this::replaced);
                    if (!remaining.isEmpty()) {
                        next.computeIfAbsent(entry.getKey(), ignored -> new TreeSet<>()).addAll(remaining);
                    } else if (!entry.getValue().isEmpty() && !next.containsKey(entry.getKey())) {
                        var directory = base.resolve("target/prefab-test-sources");
                        var target = directory.resolve(entry.getKey()).normalize();
                        if (!target.startsWith(directory) || !target.toString().endsWith(".java")) {
                            throw new IOException("Invalid generated source path in " + inventory + ": " + entry.getKey());
                        }
                        Files.deleteIfExists(target);
                    }
                }
                if (!previous.equals(next)) {
                    SourceContributions.save(inventory, next);
                }
                for (var entry : pendingIndexes.entrySet()) {
                    SourceContributions.save(entry.getKey(), entry.getValue());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot reconcile generated test sources using " + inventory, e);
            }
        });
    }
}
