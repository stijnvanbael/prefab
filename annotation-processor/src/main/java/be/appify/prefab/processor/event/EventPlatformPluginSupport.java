package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import java.util.stream.Stream;

/**
 * Utility class for event platform plugins.
 */
public class EventPlatformPluginSupport {
    private static Event.Platform derivedPlatform = null;
    private static boolean multiplePlatformsDetected = false;

    private EventPlatformPluginSupport() {
    }

    /**
     * Determines the owner type of the given event handler method.
     *
     * @param context
     *         prefab context
     * @param method
     *         event handler method
     * @return owner type manifest
     */
    public static TypeManifest ownerOf(PrefabContext context, ExecutableElement method) {
        return method.getParameters().stream()
                .flatMap(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class).stream()
                                .findFirst()
                                .map(event -> new TypeManifest(method.getEnclosingElement().asType(),
                                        context.processingEnvironment()))
                                .stream())
                .findFirst()
                .orElseThrow();
    }

    /**
     * Retrieves all event handler methods defined within components.
     *
     * @param context
     *         prefab context
     * @return stream of event handler methods
     */
    public static Stream<ExecutableElement> eventHandlers(PrefabContext context) {
        return context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(element -> (ExecutableElement) element);
    }

    /**
     * Sets the derived event platform.
     *
     * @param platform
     *         event platform to set
     */
    public static void setDerivedPlatform(Event.Platform platform) {
        if (derivedPlatform == null && !multiplePlatformsDetected) {
            derivedPlatform = platform;
        } else if (derivedPlatform != platform) {
            multiplePlatformsDetected = true;
            derivedPlatform = null;
        }
    }

    /**
     * Retrieves the derived event platform.
     *
     * @return derived event platform
     */
    public static Event.Platform derivedPlatform() {
        return derivedPlatform;
    }

    /**
     * Indicates whether multiple event platforms were detected.
     *
     * @return true if multiple platforms were detected, false otherwise
     */
    public static boolean isMultiplePlatformsDetected() {
        return multiplePlatformsDetected;
    }
}
