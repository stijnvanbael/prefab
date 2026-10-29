package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.PreviousName;
import be.appify.prefab.core.evolution.DefaultLiterals;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Reads the evolution declarations ({@link PreviousName}, {@link Default}) of an event contract. */
public final class EventEvolutionSupport {
    private EventEvolutionSupport() {
    }

    /**
     * Returns the previous names declared on a field.
     *
     * @param field the event field
     * @return the previous names, empty if none are declared
     */
    public static List<String> previousNamesOf(VariableManifest field) {
        return field.getAnnotation(PreviousName.class)
                .map(annotation -> List.of(annotation.value().value()))
                .orElseGet(List::of);
    }

    /**
     * Returns the previous names declared on an event type.
     *
     * @param type the event type
     * @return the previous names, empty if none are declared
     */
    public static List<String> previousNamesOf(TypeManifest type) {
        return type.annotationsOfType(PreviousName.class).stream()
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .toList();
    }

    /**
     * Returns the default literal declared on a field.
     *
     * @param field the event field
     * @return the literal, empty if no default is declared
     */
    public static Optional<String> defaultLiteralOf(VariableManifest field) {
        return field.getAnnotation(Default.class).map(annotation -> annotation.value().value());
    }

    /**
     * Returns the parsed default of a field. A default that fails {@link #defaultError} is ignored.
     *
     * @param field the event field
     * @return the default as a {@code String}, {@code Boolean}, {@code Integer}, {@code Long}, {@code Float},
     * {@code Double} (the constant name for enums) or an empty {@code List}; empty if no default is declared
     */
    public static Optional<Object> defaultValueOf(VariableManifest field) {
        return defaultLiteralOf(field).filter(literal -> defaultError(field).isEmpty()).map(literal -> parse(field.type(), literal));
    }

    /**
     * Validates the default declared on a field.
     *
     * @param field the event field
     * @return an error message, or empty if the field declares no default or a valid one
     */
    public static Optional<String> defaultError(VariableManifest field) {
        var literal = defaultLiteralOf(field);
        if (literal.isEmpty()) {
            return Optional.empty();
        }
        if (field.nullable()) {
            return Optional.of("Field '%s' is nullable and already defaults to null; remove @Default."
                    .formatted(field.name()));
        }
        try {
            parse(field.type(), literal.get());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of("Invalid @Default on field '%s': %s".formatted(field.name(), e.getMessage()));
        }
    }

    private static Object parse(TypeManifest type, String literal) {
        if (type.isEnum()) {
            if (!type.enumValues().contains(literal)) {
                throw new IllegalArgumentException("'%s' is not a constant of %s".formatted(literal, type));
            }
            return literal;
        }
        if (type.is(List.class)) {
            return DefaultLiterals.parse(List.class, literal);
        }
        return DefaultLiterals.parse(standardClassOf(type), literal);
    }

    private static Class<?> standardClassOf(TypeManifest type) {
        if (!type.isStandardType()) {
            throw new IllegalArgumentException(
                    "A default is not supported for %s; supported are String, numbers, boolean, enums and empty lists"
                            .formatted(type));
        }
        return type.asClass();
    }
}
