package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;

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
                        TypeManifest.of(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class).stream()
                                .findFirst()
                                .map(event -> TypeManifest.of(method.getEnclosingElement().asType(),
                                        context.processingEnvironment()))
                                .stream())
                .findFirst()
                .orElseThrow();
    }

    /**
     * Retrieves all event handler methods defined within components, excluding merged handlers (those with a
     * non-default {@link EventHandler#value()}).
     *
     * @param context
     *         prefab context
     * @return stream of event handler methods
     */
    public static Stream<ExecutableElement> eventHandlers(PrefabContext context) {
        return context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(element -> (ExecutableElement) element)
                .filter(method -> !isMergedHandler(method));
    }

    /**
     * Groups event handler methods by their owner type, applying a filter.
     *
     * @param context
     *         prefab context
     * @param filter
     *         filter function to apply to each method
     * @return map of owner type manifests to lists of event handler methods
     */
    public static Map<TypeManifest, List<ExecutableElement>> filteredEventHandlersByOwner(
            PrefabContext context,
            Function<ExecutableElement, Boolean> filter
    ) {
        return eventHandlers(context)
                .filter(filter::apply)
                .collect(Collectors.groupingBy(method -> ownerOf(context, method)));
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

    /**
     * Returns true if {@code element} is an AVSC-generated record, i.e. a {@code record} type that
     * implements an interface directly annotated with {@link Avsc}.
     * <p>
     * AVSC-generated records inherit the messaging contract from their interface. A publisher should
     * be generated for the interface only — not for every individual implementation.
     */
    public static boolean isAvscGeneratedRecord(Element element) {
        if (element.getKind() != ElementKind.RECORD) return false;
        return ((TypeElement) element).getInterfaces().stream()
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .anyMatch(iface -> iface.getAnnotation(Avsc.class) != null);
    }

    private static boolean isMergedHandler(ExecutableElement method) {
        var annotation = method.getAnnotationsByType(EventHandler.class)[0];
        try {
            return annotation.value() != void.class;
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().getKind() != TypeKind.VOID;
        }
    }
}
