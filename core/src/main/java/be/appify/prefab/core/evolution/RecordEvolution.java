package be.appify.prefab.core.evolution;

import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.PreviousName;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.node.ObjectNode;

/** The evolution declarations ({@link PreviousName}, {@link Default}) on the components of a record. */
final class RecordEvolution {
    private final List<ComponentEvolution> components;

    private RecordEvolution(List<ComponentEvolution> components) {
        this.components = components;
    }

    static Optional<RecordEvolution> of(Class<?> type) {
        if (!type.isRecord()) {
            return Optional.empty();
        }
        var evolving = Arrays.stream(type.getRecordComponents())
                .map(ComponentEvolution::of)
                .filter(ComponentEvolution::evolves)
                .toList();
        return evolving.isEmpty() ? Optional.empty() : Optional.of(new RecordEvolution(evolving));
    }

    /** Rewrites a message written under an older contract so that it matches the current contract. */
    void apply(ObjectNode message) {
        components.forEach(component -> component.apply(message));
    }

    private record ComponentEvolution(String name, List<String> previousNames, Optional<Object> defaultValue) {

        static ComponentEvolution of(RecordComponent component) {
            var previousNames = Optional.ofNullable(component.getAnnotation(PreviousName.class))
                    .map(annotation -> List.of(annotation.value()))
                    .orElseGet(List::of);
            var defaultValue = Optional.ofNullable(component.getAnnotation(Default.class))
                    .map(annotation -> DefaultLiterals.parse(component.getType(), annotation.value()));
            return new ComponentEvolution(component.getName(), previousNames, defaultValue);
        }

        boolean evolves() {
            return !previousNames.isEmpty() || defaultValue.isPresent();
        }

        void apply(ObjectNode message) {
            if (message.has(name)) {
                return;
            }
            previousNames.stream()
                    .filter(message::has)
                    .findFirst()
                    .ifPresentOrElse(
                            previousName -> message.set(name, message.remove(previousName)),
                            () -> defaultValue.ifPresent(value -> putDefault(message, value)));
        }

        private void putDefault(ObjectNode message, Object value) {
            switch (value) {
                case String string -> message.put(name, string);
                case Boolean bool -> message.put(name, bool);
                case Integer integer -> message.put(name, integer);
                case Long number -> message.put(name, number);
                case Float number -> message.put(name, number);
                case Double number -> message.put(name, number);
                case List<?> ignored -> message.putArray(name);
                default -> throw new IllegalStateException("Unsupported default value: " + value);
            }
        }
    }
}
