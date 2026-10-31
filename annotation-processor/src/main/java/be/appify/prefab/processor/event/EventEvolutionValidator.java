package be.appify.prefab.processor.event;


import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static be.appify.prefab.processor.event.EventEvolutionSupport.defaultError;
import static be.appify.prefab.processor.event.EventEvolutionSupport.previousNamesOf;

/**
 * Rejects evolution declarations that cannot be satisfied: defaults that do not match the component type, and
 * previous names that collide with a current name.
 */
class EventEvolutionValidator {
    private final PrefabContext context;
    private final Set<String> validated = new HashSet<>();

    EventEvolutionValidator(PrefabContext context) {
        this.context = context;
    }

    void validate(TypeManifest event, Set<String> currentEventNames) {
        if (!validated.add(event.asTypeName().toString())) {
            return;
        }
        validateTypeNames(event, currentEventNames);
        validateFields(event);
        event.permittedSubtypes().forEach(subtype -> validate(subtype, currentEventNames));
    }

    private void validateTypeNames(TypeManifest type, Set<String> currentEventNames) {
        var previousNames = previousNamesOf(type);
        if (previousNames.isEmpty()) {
            return;
        }
        if (previousNames.stream().anyMatch(String::isBlank)) {
            error(type, "@PreviousName on '%s' must not contain a blank name.".formatted(type.simpleName()));
        }
        if (new HashSet<>(previousNames).size() != previousNames.size()) {
            error(type, "@PreviousName on '%s' contains duplicate names.".formatted(type.simpleName()));
        }
        previousNames.stream()
                .filter(currentEventNames::contains)
                .forEach(name -> error(type, ("@PreviousName '%s' on '%s' collides with the name of a current "
                        + "event type.").formatted(name, type.simpleName())));
    }

    private void validateFields(TypeManifest type) {
        var fields = type.fields();
        var currentNames = fields.stream().map(VariableManifest::name).toList();
        var claimed = new HashSet<String>();
        fields.forEach(field -> {
            validateDefault(field);
            validatePreviousNames(field, currentNames, claimed);
        });
    }

    private void validateDefault(VariableManifest field) {
        defaultError(field).ifPresent(message -> context.logError(message, field.element()));
    }

    private void validatePreviousNames(VariableManifest field, List<String> currentNames, Set<String> claimed) {
        var previousNames = previousNamesOf(field);
        if (previousNames.stream().anyMatch(String::isBlank)) {
            context.logError("@PreviousName on field '%s' must not contain a blank name.".formatted(field.name()),
                    field.element());
        }
        previousNames.forEach(name -> {
            if (currentNames.contains(name)) {
                context.logError("@PreviousName '%s' on field '%s' collides with the current field '%s'."
                        .formatted(name, field.name(), name), field.element());
            } else if (!claimed.add(name)) {
                context.logError("@PreviousName '%s' on field '%s' is also declared as previous name of another field."
                        .formatted(name, field.name()), field.element());
            }
        });
    }

    private void error(TypeManifest type, String message) {
        context.logError(message, type.asElement());
    }

    static Set<String> namesOf(TypeManifest type) {
        var simpleName = type.simpleName();
        var lastSegment = simpleName.substring(simpleName.lastIndexOf('.') + 1);
        var prefix = type.packageName().isEmpty() ? "" : type.packageName() + ".";
        return new HashSet<>(List.of(simpleName, lastSegment, prefix + simpleName,
                prefix + simpleName.replace('.', '$')));
    }
}