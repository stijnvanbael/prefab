package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.lang.annotation.Annotation;
import javax.lang.model.element.ExecutableElement;

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
}
