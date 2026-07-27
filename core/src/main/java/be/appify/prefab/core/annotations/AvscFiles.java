package be.appify.prefab.core.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Utilities for normalising the schema declarations on {@link Avsc}. */
public final class AvscFiles {

    private AvscFiles() {
    }

    public static Resolution resolve(Avsc avsc) {
        var definitions = new LinkedHashMap<String, Definition>();
        var errors = new ArrayList<String>();
        Arrays.stream(avsc.value()).forEach(path -> addDefinition(path, Optional.empty(), definitions, errors));
        Arrays.stream(avsc.files()).forEach(file -> addDefinition(
                file.path(),
                blankToEmpty(file.keyProperty()),
                definitions,
                errors));
        if (definitions.isEmpty() && errors.isEmpty()) {
            errors.add("@Avsc requires at least one schema declared via value() or files().");
        }
        return new Resolution(List.copyOf(definitions.values()), List.copyOf(errors));
    }

    private static void addDefinition(String rawPath, Optional<String> keyProperty,
                                      LinkedHashMap<String, Definition> definitions, List<String> errors) {
        var path = rawPath == null ? "" : rawPath.trim();
        if (path.isEmpty()) {
            errors.add("@Avsc schema path must not be blank.");
            return;
        }
        var definition = new Definition(path, keyProperty);
        if (definitions.putIfAbsent(path, definition) != null) {
            errors.add("AVSC file '%s' is declared multiple times on @Avsc. Declare each schema only once across value() and files()."
                    .formatted(path));
        }
    }

    private static Optional<String> blankToEmpty(String value) {
        if (value == null) {
            return Optional.empty();
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    public record Resolution(List<Definition> definitions, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public record Definition(String path, Optional<String> keyProperty) {
        public Definition {
            keyProperty = keyProperty == null ? Optional.empty() : keyProperty;
        }
    }
}
