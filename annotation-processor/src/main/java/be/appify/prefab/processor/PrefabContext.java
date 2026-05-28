package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.OutputTarget;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    /**
     * Selects which event set a plugin wants to inspect.
     */
    public enum EventScope {
        CURRENT_COMPILATION,
        CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES
    }

    private final ProcessingEnvironment processingEnvironment;
    private final RequestParameterBuilder requestParameterBuilder;
    private final List<PrefabPlugin> plugins;
    private final RequestParameterMapper requestParameterMapper;
    private final RoundEnvironment roundEnvironment;
    private final Set<ExecutableElement> inheritedDeferredEventHandlers;
    private final Set<ExecutableElement> newlyDeferredEventHandlers = new LinkedHashSet<>();
    private final Set<String> currentCompilationTypeNames;
    private final GenerateAnnotationValidator generateAnnotationValidator;

    // Memoized per-round event views; each computed at most once per PrefabContext instance.
    private List<TypeElement> memoizedCurrentCompilationEventElements;
    private List<TypeElement> memoizedCurrentAndConsumedEventElements;

    // Cached plugin override registries per TypeElement
    private final Map<String, PluginOverrideRegistry> pluginOverridesByType = new HashMap<>();

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
        this.generateAnnotationValidator = new GenerateAnnotationValidator(processingEnvironment);
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
     * Returns event type elements that belong to the current compilation only.
     *
     * <p>This is the safe default for plugin authors generating local code or test support. It
     * includes both source-declared {@link Event} types and records generated from
     * {@link Avsc}-annotated contracts in the current compilation.
     *
     * @return deduplicated stream of current-compilation event types
     */
    public Stream<TypeElement> eventElements() {
        return eventElements(EventScope.CURRENT_COMPILATION);
    }

    /**
     * Returns event type elements for the requested scope.
     *
     * @param scope
     *         event discovery scope
     * @return deduplicated stream of matching event types
     */
    public Stream<TypeElement> eventElements(EventScope scope) {
        return switch (scope) {
            case CURRENT_COMPILATION -> currentCompilationEventElements().stream();
            case CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES ->
                    currentAndConsumedEventElements().stream();
        };
    }

    /**
     * Returns event types from the current compilation plus dependency events consumed through local
     * {@link EventHandler} signatures.
     *
     * <p>Use this explicit opt-in when a plugin must generate infrastructure for dependency events,
     * such as producer or documentation artefacts.
     *
     * @return deduplicated stream of local and consumed dependency event types
     */
    public Stream<TypeElement> eventElementsIncludingConsumedDependencies() {
        return eventElements(EventScope.CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES);
    }

    private List<TypeElement> currentCompilationEventElements() {
        if (memoizedCurrentCompilationEventElements == null) {
            memoizedCurrentCompilationEventElements = computeCurrentCompilationEventElements();
        }
        return memoizedCurrentCompilationEventElements;
    }

    private List<TypeElement> currentAndConsumedEventElements() {
        if (memoizedCurrentAndConsumedEventElements == null) {
            memoizedCurrentAndConsumedEventElements = computeCurrentAndConsumedEventElements();
        }
        return memoizedCurrentAndConsumedEventElements;
    }

    private List<TypeElement> computeCurrentCompilationEventElements() {
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

        return Stream.concat(annotated, avscGenerated)
                .distinct()
                .toList();
    }

    private List<TypeElement> computeCurrentAndConsumedEventElements() {
        return Stream.concat(currentCompilationEventElements().stream(), eventElementsFromClasspath())
                .distinct()
                .toList();
    }

    /**
     * Returns event type elements that belong to the currently compiled module only.
     *
     * @return deduplicated stream of local event types
     */
    public Stream<TypeElement> eventElementsFromCurrentCompilation() {
        return eventElements();
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
     * Returns the plugin override registry for an aggregate class.
     *
     * <p>Reads and validates any {@code @Generate} annotations on the class, building a registry
     * of overrides. Results are cached for the lifetime of this context.
     *
     * @param aggregateType
     *         the aggregate type element
     * @return a registry of plugin overrides (empty if none are defined)
     */
    public PluginOverrideRegistry pluginOverridesFor(TypeElement aggregateType) {
        String typeName = aggregateType.getQualifiedName().toString();
        return pluginOverridesByType.computeIfAbsent(
                typeName,
                k -> generateAnnotationValidator.validateAndBuildRegistry(aggregateType)
        );
    }

    /**
     * Check whether a plugin is enabled for an aggregate.
     *
     * <p>Queries the plugin override registry for the given type element.
     *
     * @param aggregateType
     *         the aggregate type element
     * @param pluginClass
     *         the plugin class to check
     * @return true if the plugin should be enabled
     */
    public boolean isPluginEnabledFor(TypeElement aggregateType, Class<?> pluginClass) {
        return pluginOverridesFor(aggregateType).isPluginEnabled(pluginClass);
    }

    /**
     * Get the output target for a plugin on an aggregate.
     *
     * <p>Queries the plugin override registry for the given type element.
     *
     * @param aggregateType
     *         the aggregate type element
     * @param pluginClass
     *         the plugin class to check
     * @return the output target (never null)
     */
    public OutputTarget getOutputTargetFor(TypeElement aggregateType, Class<?> pluginClass) {
        return pluginOverridesFor(aggregateType).getOutputTarget(pluginClass);
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
     * Returns the configured builder setter-method prefix.
     *
     * <p>Read from the {@code prefab.builder.setterPrefix} annotation-processor option
     * (pass via {@code -Aprefab.builder.setterPrefix=} on the compiler command line or via the
     * Maven {@code compilerArg} / Gradle {@code compilerArgs} build configuration).
     * Defaults to {@code ""} when the option is absent.
     * An empty string produces prefix-less setter names equal to the field name.
     *
     * @return the builder setter prefix, never {@code null}
     */
    public String builderSetterPrefix() {
        return processingEnvironment.getOptions().getOrDefault("prefab.builder.setterPrefix", "");
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
