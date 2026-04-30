package be.appify.prefab.processor.event.handler;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static javax.lang.model.type.TypeKind.VOID;

/** Interface for event handler plugins. */
public interface EventHandlerPlugin extends PrefabPlugin {
    /**
     * The annotation class that this plugin handles.
     *
     * @return The annotation class that this plugin handles.
     */
    Class<? extends Annotation> annotation();

    /**
     * Get the event type handled by the given method.
     *
     * @param element
     *         The method element.
     * @param context
     *         The Prefab context.
     * @return The event type.
     */
    default TypeManifest getEventType(ExecutableElement element, PrefabContext context) {
        var parameters = element.getParameters();
        if (parameters.size() != 1) {
            context.logError(
                    "Domain event handler method %s must have exactly one parameter".formatted(element),
                    element);
        }
        var eventType = TypeManifest.of(parameters.getFirst().asType(), context.processingEnvironment());
        if (eventType.asElement() == null) {
            context.logError(
                    "Domain event handler method %s must have a parameter that is a declared class or record".formatted(
                            element),
                    element
            );
        }
        return eventType;
    }

    /**
     * Finds the static companion {@code @EventHandler} method on {@code typeElement} that handles the given
     * {@code eventType} and returns the aggregate itself (not void, not Optional). At most one such method is
     * expected; if multiple are found, a compiler error is reported.
     *
     * @param typeElement
     *         the aggregate type element to search for the static companion
     * @param eventType
     *         the event type that the companion handles
     * @param context
     *         the Prefab context
     * @return the name of the static companion method, or empty if none exists
     */
    default Optional<String> findStaticCompanion(TypeElement typeElement, TypeManifest eventType,
            PrefabContext context) {
        var candidates = typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.class).length > 0)
                .filter(element -> element.getParameters().size() == 1)
                .filter(element -> {
                    var paramType = TypeManifest.of(element.getParameters().getFirst().asType(),
                            context.processingEnvironment());
                    return paramType.asElement() != null
                            && paramType.asElement().equals(eventType.asElement());
                })
                .filter(element -> {
                    var returnType = TypeManifest.of(element.getReturnType(), context.processingEnvironment());
                    return element.getReturnType().getKind() != VOID
                            && !returnType.is(Optional.class)
                            && Objects.equals(returnType.asElement(), typeElement);
                })
                .toList();
        if (candidates.size() > 1) {
            candidates.forEach(element -> context.logError(
                    "Multiple static @EventHandler methods found for event type %s; at most one static companion is allowed per event type".formatted(
                            eventType.asElement().getSimpleName()),
                    element));
            return Optional.empty();
        }
        return candidates.stream().map(element -> element.getSimpleName().toString()).findFirst();
    }
}
