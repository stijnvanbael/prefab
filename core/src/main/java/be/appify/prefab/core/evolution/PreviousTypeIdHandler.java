package be.appify.prefab.core.evolution;

import be.appify.prefab.core.annotations.PreviousName;
import java.util.Arrays;
import java.util.Optional;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.jsontype.TypeIdResolver;

/** Resolves JSON type ids that match a {@link PreviousName} of an event type to the current type. */
final class PreviousTypeIdHandler extends DeserializationProblemHandler {

    @Override
    public JavaType handleUnknownTypeId(
            DeserializationContext context,
            JavaType baseType,
            String subTypeId,
            TypeIdResolver idResolver,
            String failureMessage
    ) {
        return typeWithPreviousName(baseType.getRawClass(), subTypeId)
                .map(context::constructType)
                .orElse(null);
    }

    private static Optional<Class<?>> typeWithPreviousName(Class<?> type, String name) {
        var annotation = type.getAnnotation(PreviousName.class);
        if (annotation != null && Arrays.asList(annotation.value()).contains(name)) {
            return Optional.of(type);
        }
        if (!type.isSealed()) {
            return Optional.empty();
        }
        return Arrays.stream(type.getPermittedSubclasses())
                .flatMap(subtype -> typeWithPreviousName(subtype, name).stream())
                .findFirst();
    }
}
