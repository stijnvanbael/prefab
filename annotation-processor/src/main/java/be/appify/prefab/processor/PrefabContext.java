package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;

/**
 * Context class providing access to processing environment, plugins, and utilities during annotation processing.
 */
public class PrefabContext {
    private final ProcessingEnvironment processingEnvironment;
    private final RequestParameterBuilder requestParameterBuilder;
    private final List<PrefabPlugin> plugins;
    private final RequestParameterMapper requestParameterMapper;
    private final RoundEnvironment roundEnvironment;
    private final Set<ExecutableElement> inheritedDeferredEventHandlers;
    private final Set<ExecutableElement> newlyDeferredEventHandlers = new LinkedHashSet<>();
    private final Set<String> currentCompilationTypeNames;

    /**
     * Constructs a PrefabContext.
     *
     * @param processingEnvironment
     *         the processing environment
     * @param plugins
     *         the list of Prefab plugins
     * @param roundEnvironment
     *         the round environment
     */
    public PrefabContext(
            ProcessingEnvironment processingEnvironment,
            List<PrefabPlugin> plugins,
            RoundEnvironment roundEnvironment
    ) {
        this(processingEnvironment, plugins, roundEnvironment, Set.of(), Set.of());
    }

    /**
     * Constructs a PrefabContext with event handlers deferred from a previous round.
     *
     * @param processingEnvironment
     *         the processing environment
     * @param plugins
     *         the list of Prefab plugins
     * @param roundEnvironment
     *         the round environment
     * @param inheritedDeferredEventHandlers
     *         event handlers deferred from a previous processing round
     */
    public PrefabContext(
            ProcessingEnvironment processingEnvironment,
            List<PrefabPlugin> plugins,
            RoundEnvironment roundEnvironment,
            Set<ExecutableElement> inheritedDeferredEventHandlers
    ) {
        this(processingEnvironment, plugins, roundEnvironment, inheritedDeferredEventHandlers, Set.of());
    }

    /**
     * Constructs a PrefabContext with event handlers deferred from a previous round and all known
     * type names that belong to the current compilation (across rounds).
     *
     * @param processingEnvironment
     *         the processing environment
     * @param plugins
     *         the list of Prefab plugins
     * @param roundEnvironment
     *         the round environment
     * @param inheritedDeferredEventHandlers
     *         event handlers deferred from a previous processing round
     * @param currentCompilationTypeNames
     *         all type names seen as root elements in this compilation
     */
    public PrefabContext(
            ProcessingEnvironment processingEnvironment,
            List<PrefabPlugin> plugins,
            RoundEnvironment roundEnvironment,
            Set<ExecutableElement> inheritedDeferredEventHandlers,
            Set<String> currentCompilationTypeNames
    ) {
        this.processingEnvironment = processingEnvironment;
        this.plugins = plugins;
        this.roundEnvironment = roundEnvironment;
        this.inheritedDeferredEventHandlers = Set.copyOf(inheritedDeferredEventHandlers);
        this.currentCompilationTypeNames = Set.copyOf(currentCompilationTypeNames);
        requestParameterBuilder = new RequestParameterBuilder(plugins);
        requestParameterMapper = new RequestParameterMapper(plugins);
    }

    /**
     * Gets the RequestParameterBuilder.
     *
     * @return the RequestParameterBuilder
     */
    public RequestParameterBuilder requestParameterBuilder() {
        return requestParameterBuilder;
    }

    /**
     * Gets the ProcessingEnvironment.
     *
     * @return the ProcessingEnvironment
     */
    public ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    /**
     * Gets the RoundEnvironment.
     *
     * @return the RoundEnvironment
     */
    public RoundEnvironment roundEnvironment() {
        return roundEnvironment;
    }

    /**
     * Gets the list of Prefab plugins.
     *
     * @return the list of Prefab plugins
     */
    public List<PrefabPlugin> plugins() {
        return plugins;
    }

    /**
     * Gets the RequestParameterMapper.
     *
     * @return the RequestParameterMapper
     */
    public RequestParameterMapper requestParameterMapper() {
        return requestParameterMapper;
    }

    /**
     * Returns the event handlers that were deferred from a previous processing round.
     *
     * @return inherited deferred event handlers
     */
    public Set<ExecutableElement> inheritedDeferredEventHandlers() {
        return inheritedDeferredEventHandlers;
    }

    /**
     * Defers an event handler to the next processing round.
     * Call this when the concrete type for an {@code @Avsc} event is not yet available.
     *
     * @param handler
     *         event handler method to defer
     */
    public void deferEventHandler(ExecutableElement handler) {
        newlyDeferredEventHandlers.add(handler);
    }

    /**
     * Returns the event handlers that were deferred during this processing round.
     * These should be passed to the next round's {@link PrefabContext}.
     *
     * @return newly deferred event handlers
     */
    public Set<ExecutableElement> newlyDeferredEventHandlers() {
        return Set.copyOf(newlyDeferredEventHandlers);
    }

    /**
     * Returns all event type elements for the current round: both types directly annotated with
     * {@link Event} and records generated from {@link Avsc}-annotated contract interfaces.
     *
     * <p>AVSC-generated records implement an {@code @Avsc}-annotated interface but may not be
     * returned by {@link RoundEnvironment#getElementsAnnotatedWith(Class)} in the same round they
     * are compiled. Scanning {@link RoundEnvironment#getRootElements()} for records that implement
     * such an interface is the reliable strategy used throughout the framework.
     *
     * @return a deduplicated stream of {@link TypeElement}s representing all events
     */
    public Stream<TypeElement> eventElements() {
        var annotated = roundEnvironment.getElementsAnnotatedWith(Event.class)
                .stream()
                .map(e -> (TypeElement) e);

        var avscGenerated = roundEnvironment.getRootElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.RECORD)
                .map(e -> (TypeElement) e)
                .filter(r -> r.getInterfaces().stream()
                        .map(i -> (TypeElement) ((DeclaredType) i).asElement())
                        .anyMatch(i -> i.getAnnotation(Avsc.class) != null));

        var fromClasspath = eventElementsFromClasspath();

        return Stream.concat(Stream.concat(annotated, avscGenerated), fromClasspath).distinct();
    }

    /**
     * Returns event type elements that belong to the currently compiled module only.
     *
     * @return deduplicated stream of local event types
     */
    public Stream<TypeElement> eventElementsFromCurrentCompilation() {
        return eventElements().filter(this::isFromCurrentCompilation);
    }

    /**
     * Returns {@code @Avsc}-annotated interfaces that belong to the currently compiled module.
     *
     * @return stream of local {@code @Avsc}-annotated interfaces
     */
    public Stream<TypeElement> avscElementsFromCurrentCompilation() {
        return roundEnvironment.getElementsAnnotatedWith(Avsc.class)
                .stream()
                .map(TypeElement.class::cast)
                .filter(this::isFromCurrentCompilation);
    }

    /**
     * Returns whether the given type belongs to the current compilation unit (not dependency
     * classpath input).
     * <p>
     * Nested types (e.g. a sealed interface declared inside an {@code @Aggregate} record) are
     * considered part of the current compilation when their enclosing top-level type is.
     *
     * @param type
     *         the type to check
     * @return true when the type originates from current compilation rounds
     */
    public boolean isFromCurrentCompilation(TypeElement type) {
        if (currentCompilationTypeNames.contains(type.getQualifiedName().toString())) {
            return true;
        }
        var enclosing = type.getEnclosingElement();
        if (enclosing instanceof TypeElement enclosingType) {
            return isFromCurrentCompilation(enclosingType);
        }
        return false;
    }

    private Stream<TypeElement> eventElementsFromClasspath() {
        return roundEnvironment.getElementsAnnotatedWith(EventHandler.class)
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .flatMap(method -> method.getParameters().stream())
                .map(VariableElement::asType)
                .filter(type -> type.getKind().name().equals("DECLARED"))
                .map(type -> (TypeElement) ((DeclaredType) type).asElement())
                .filter(type -> type.getAnnotation(Event.class) != null);
    }

    /**
     * Logs an error message associated with a specific element.
     *
     * @param message
     *         the error message
     * @param element
     *         the element associated with the error
     */
    public void logError(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Logs a note message associated with a specific element.
     *
     * @param message
     *         the note message
     * @param element
     *         the element associated with the note
     */
    public void logNote(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }
}
