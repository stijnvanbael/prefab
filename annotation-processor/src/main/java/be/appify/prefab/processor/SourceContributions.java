package be.appify.prefab.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.TypeElement;

/**
 * Module-local inputs to one aggregating writer. Recompiling a source replaces its contributions,
 * including when its last annotation was removed. Classpath dependencies never become local owners.
 *
 * @param <T> immutable, JSON-serializable description of one contribution
 */
public final class SourceContributions<T> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<String, List<T>> current = new TreeMap<>();
    private final Class<T> type;
    private Map<String, Owned<T>> previous;
    private boolean removedLast;

    /** @param type the type of a contribution; its qualified name also namespaces the build metadata */
    public SourceContributions(Class<T> type) {
        this.type = type;
    }

    /** Records an input against the top-level declaration of its contributing source file. */
    public void add(TypeElement owner, T value) {
        while (owner.getEnclosingElement() instanceof TypeElement enclosing) {
            owner = enclosing;
        }
        var values = current.computeIfAbsent(owner.getQualifiedName().toString(), ignored -> new ArrayList<>());
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    /** Returns fresh inputs together with those of unchanged owners in this compiler output. */
    public Map<String, List<T>> merge(PrefabContext context) {
        var inventory = context.incrementalState();
        var index = inventory.indexPath(type.getName());
        if (index.isEmpty()) {
            return Collections.unmodifiableMap(current);
        }
        var path = index.get();
        try {
            if (previous == null) {
                var factory = JSON.getTypeFactory();
                var mapType = factory.constructMapType(TreeMap.class, factory.constructType(String.class),
                        factory.constructParametricType(Owned.class, type));
                previous = Files.isRegularFile(path) ? JSON.readValue(path.toFile(), mapType) : Map.of();
            }
            var merged = new TreeMap<>(previous);
            merged.values().removeIf(owner -> inventory.replaced(owner.source()));
            current.forEach((name, values) -> inventory.sourceOf(name)
                    .ifPresent(source -> merged.put(name, new Owned<>(source, List.copyOf(values)))));
            removedLast = !previous.isEmpty() && merged.isEmpty();
            if (!previous.equals(merged)) {
                inventory.saveOnSuccess(path, merged);
            }
            var result = new TreeMap<String, List<T>>();
            merged.forEach((name, owned) -> result.put(name, owned.values()));
            current.forEach(result::putIfAbsent);
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot update compiler input index " + path, e);
        }
    }

    /** Whether this merge removed the last previously indexed owner. */
    public boolean removedLast() {
        return removedLast;
    }

    private record Owned<T>(String source, List<T> values) {}

    static void save(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        var temporary = Files.createTempFile(path.getParent(), "prefab-", ".tmp");
        try {
            JSON.writeValue(temporary.toFile(), value);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
