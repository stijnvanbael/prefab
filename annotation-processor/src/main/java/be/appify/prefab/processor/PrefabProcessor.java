package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventSnapshot;
import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Streaming;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.util.FullTextLanguages;
import be.appify.prefab.processor.error.ErrorReporter;
import be.appify.prefab.processor.handwritten.HandWrittenFindings;
import be.appify.prefab.processor.error.ProcessingGuard;
import be.appify.prefab.processor.event.EventBindings;
import com.google.auto.service.AutoService;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

/**
 * Annotation processor for Prefab framework that generates code based on annotated aggregates.
 */
@SupportedAnnotationTypes({
        "be.appify.prefab.core.annotations.*",
        "be.appify.prefab.core.annotations.rest.Update",
})
@SupportedOptions({"prefab.builder.setterPrefix", FullTextLanguages.OPTION, "prefab.events.baselineDir",
        "prefab.events.compatibility", ErrorReporter.STACKTRACE_OPTION, HandWrittenFindings.SEVERITY_OPTION})
@AutoService(Processor.class)
@SuppressWarnings("unused")
public class PrefabProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    // Aggregates whose field types were unresolved in a previous round.
    // AbstractProcessor is instantiated once per compilation, so instance state persists across rounds.
    private final Set<TypeElement> deferredAggregates = new LinkedHashSet<>();
    private final Set<TypeElement> deferredPolymorphicAggregates = new LinkedHashSet<>();
    private final Set<ExecutableElement> deferredEventHandlers = new LinkedHashSet<>();

    // All aggregate manifests resolved across all rounds, used for global file generation.
    private final List<ClassManifest> allResolvedAggregates = new ArrayList<>();
    private final List<PolymorphicAggregateManifest> allResolvedPolymorphicAggregates = new ArrayList<>();
    private boolean globalFilesWritten = false;
    private boolean eventFilesWritten = false;
    private final ViewWriter viewWriter = new ViewWriter();

    // Plugin list is stable for the lifetime of the processor; detect once and reuse across rounds.
    private List<PrefabPlugin> cachedPlugins;

    /** Creates a processor that discovers its plugins with {@link ServiceLoader}. */
    public PrefabProcessor() {
    }

    /**
     * Creates a processor with an explicit set of plugins instead of {@link ServiceLoader} discovery.
     *
     * @param plugins the plugins to run; they are executed in {@link PrefabPlugin#EXECUTION_ORDER}
     */
    PrefabProcessor(List<PrefabPlugin> plugins) {
        cachedPlugins = sorted(plugins.stream());
    }

    // Tracking sets for deduplication across rounds (source elements are visible in every round).
    private final Set<String> processedAggregateNames = new LinkedHashSet<>();
    private final Set<String> failedAggregateNames = new LinkedHashSet<>();
    private final Set<String> processedPolymorphicAggregateNames = new LinkedHashSet<>();
    private final Set<String> processedAdditionalFilesAggregates = new LinkedHashSet<>();
    private final Set<String> processedAdditionalFilesPolymorphicAggregates = new LinkedHashSet<>();
    private final Set<String> seenEventElementNames = new LinkedHashSet<>();
    private final Set<String> currentCompilationTypeNames = new LinkedHashSet<>();
    private final IncrementalBuildState incrementalState = new IncrementalBuildState();

    // Plugins whose initContext failed; calling them again would only produce consequent errors.
    private final Set<PrefabPlugin> failedPlugins = new LinkedHashSet<>();

    private static final String PROCESSOR_COMPONENT = "the Prefab annotation processor";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        var guard = new ProcessingGuard(new ErrorReporter(processingEnv));
        guard.run(PROCESSOR_COMPONENT, null, () -> processRound(environment));
        return true;
    }

    private void processRound(RoundEnvironment environment) {
        clearRoundCaches();
        warnAboutVoidUpdates(environment);
        var plugins = detectPlugins();
        rememberCurrentCompilationTypeNames(environment);
        var context = new PrefabContext(
                processingEnv,
                plugins,
                environment,
                deferredEventHandlers,
                currentCompilationTypeNames,
                incrementalState);
        incrementalState.observe(context);
        var validEvents = context.eventElementsFromCurrentCompilation()
                .map(element -> EventBindings.validate(element, context))
                .toList().stream().allMatch(Boolean::booleanValue);
        var validHandlers = environment.getElementsAnnotatedWith(EventHandler.class).stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .map(handler -> EventBindings.validateHandler(handler, context))
                .toList().stream().allMatch(Boolean::booleanValue);
        if (!validEvents || !validHandlers) {
            return;
        }
        plugins.forEach(plugin -> {
            if (!runPlugin(context, plugin, p -> p.initContext(context))) {
                failedPlugins.add(plugin);
            }
        });
        if (hasAvscAnnotations(context) && !eventFilesWritten) {
            eventFilesWritten = true;
            plugins.stream()
                    .filter(plugin -> context.configuration().isPluginEnabled(plugin.getClass()))
                    .forEach(plugin -> runPlugin(context, plugin, PrefabPlugin::writeEventFiles));
        }
        var aggregates = resolveAggregates(context, environment);
        var polymorphicAggregates = resolvePolymorphicAggregates(context, environment);
        // Validate @Generate annotations eagerly to emit compile errors early
        validateGenerateAnnotationsEagerly(context, environment);
        validateComputedMethods(environment);
        validateSecurityAnnotations(environment);
        var writtenAggregates = writeAggregates(context, aggregates);
        var writtenPolymorphicAggregates = writePolymorphicAggregates(context, polymorphicAggregates);
        context.guard().run(PROCESSOR_COMPONENT, null, () -> viewWriter.writeViews(context, environment));
        writeAdditionalFilesIfNeeded(context, plugins, writtenAggregates, writtenPolymorphicAggregates);
        allResolvedAggregates.addAll(writtenAggregates);
        allResolvedPolymorphicAggregates.addAll(writtenPolymorphicAggregates);
        if (!globalFilesWritten && deferredAggregates.isEmpty() && deferredPolymorphicAggregates.isEmpty()) {
            globalFilesWritten = true;
            writeGlobalFiles(context, plugins);
        }
        deferredEventHandlers.clear();
        deferredEventHandlers.addAll(context.newlyDeferredEventHandlers());
        if (environment.processingOver() && !environment.errorRaised()) {
            var finished = true;
            for (var plugin : plugins) {
                // A globally disabled plugin may still have been enabled on a single aggregate.
                if (!runPlugin(context, plugin, PrefabPlugin::finishProcessing)) {
                    finished = false;
                }
            }
            if (finished && failedAggregateNames.isEmpty()) {
                incrementalState.finish();
            }
        }
    }

    private boolean runPlugin(PrefabContext context, PrefabPlugin plugin, Consumer<PrefabPlugin> hook) {
        return !failedPlugins.contains(plugin)
                && context.guard().run("plugin " + plugin.getClass().getName(), null, () -> hook.accept(plugin));
    }

    private void clearRoundCaches() {
        // Only evict ERROR-kind (unresolved) type mirrors; stable, fully-resolved types survive across rounds.
        // Class-keyed and TypeElement-keyed caches never need eviction because those keys are stable.
        TypeManifest.clearUnresolvedTypeCache();
    }

    private void warnAboutVoidUpdates(RoundEnvironment environment) {
        environment.getElementsAnnotatedWith(Update.class).stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getReturnType().getKind() == TypeKind.VOID)
                .forEach(method -> processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "[@Update] " + method.getSimpleName() + ": void updates are deprecated and will be removed "
                                + "in the next major version. Return an aggregate instance instead; for async commits, "
                                + "publish the event and return the current aggregate.",
                        method));
    }

    private void rememberCurrentCompilationTypeNames(RoundEnvironment environment) {
        environment.getRootElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .map(type -> type.getQualifiedName().toString())
                .forEach(currentCompilationTypeNames::add);
    }

    /**
     * Validate all @Generate annotations on discovered aggregates. This ensures compilation
     * errors are emitted early, before any code generation happens.
     */
    private void validateGenerateAnnotationsEagerly(PrefabContext context, RoundEnvironment environment) {
        environment.getElementsAnnotatedWith(Aggregate.class).stream()
                .filter(e -> e.getKind().isClass())
                .map(TypeElement.class::cast)
                .forEach(context::pluginOverridesFor);
    }

    /**
     * Validate all @Computed methods: they must be public, take no arguments, return a value and not clash with a
     * field of the enclosing type, otherwise the generated response record would not compile or Jackson could not
     * serialize the synthetic field.
     */
    private void validateComputedMethods(RoundEnvironment environment) {
        environment.getElementsAnnotatedWith(Computed.class).stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .forEach(method -> {
                    if (!method.getModifiers().contains(Modifier.PUBLIC)
                            || !method.getParameters().isEmpty()
                            || method.getReturnType().getKind() == TypeKind.VOID) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "[@Computed] " + method.getSimpleName() + ": method must be public, take no arguments, and return a value.\n"
                                        + "Suggested fix: declare the method as: public <ReturnType> " + method.getSimpleName() + "() { ... }",
                                method);
                    }
                    var clashesWithField = method.getEnclosingElement().getEnclosedElements().stream()
                            .anyMatch(element -> element.getKind() == ElementKind.FIELD
                                    && element.getSimpleName().contentEquals(method.getSimpleName()));
                    if (clashesWithField) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "[@Computed] " + method.getSimpleName() + ": method name clashes with a field of the same name.\n"
                                        + "Suggested fix: rename either the field or the method to avoid the conflict",
                                method);
                    }
                });
    }

    private void validateSecurityAnnotations(RoundEnvironment environment) {
        validateSecurityAnnotation(environment, Autocomplete.class, Autocomplete::security);
        validateSecurityAnnotation(environment, Create.class, Create::security);
        validateSecurityAnnotation(environment, Delete.class, Delete::security);
        validateSecurityAnnotation(environment, Download.class, Download::security);
        validateSecurityAnnotation(environment, GetById.class, GetById::security);
        validateSecurityAnnotation(environment, GetList.class, GetList::security);
        validateSecurityAnnotation(environment, Streaming.class, Streaming::security);
        validateSecurityAnnotation(environment, Update.class, Update::security);
    }

    private <A extends Annotation> void validateSecurityAnnotation(
            RoundEnvironment environment,
            Class<A> annotationType,
            Function<A, Security> securityExtractor
    ) {
        environment.getElementsAnnotatedWith(annotationType).forEach(element -> {
            var annotation = element.getAnnotation(annotationType);
            if (annotation != null && !UnresolvedAnnotations.hasUnresolvedAnnotation(element)) {
                validateSecurityConfiguration(element, securityExtractor.apply(annotation));
            }
        });
    }

    private void validateSecurityConfiguration(javax.lang.model.element.Element element, Security security) {
        if (!security.authority().isBlank() && !security.role().isBlank()) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "[@Security] " + element.getSimpleName() + ": only one of authority or role can be specified, not both.\n"
                            + "Suggested fix: remove either the authority() or role() attribute from @Security",
                    element);
        }
    }

    private boolean hasAvscAnnotations(PrefabContext context) {
        return context.avscElementsFromCurrentCompilation().findAny().isPresent()
                || !context.roundEnvironment().getElementsAnnotatedWith(EventSnapshot.class).isEmpty();
    }

    private List<ClassManifest> resolveAggregates(PrefabContext context, RoundEnvironment environment) {
        var aggregateElements = Stream.concat(
                        environment.getElementsAnnotatedWith(Aggregate.class).stream()
                                .filter(e -> e.getKind().isClass() && !e.getModifiers().contains(Modifier.ABSTRACT))
                                .map(TypeElement.class::cast),
                        deferredAggregates.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var aggregates = aggregateElements.stream()
                .filter(element -> !ClassManifest.hasUnresolvedFields(element))
                .flatMap(element -> guarded(context, element, () -> ClassManifest.of(element, processingEnv)))
                .toList();

        deferredAggregates.clear();
        aggregateElements.stream()
                .filter(ClassManifest::hasUnresolvedFields)
                .forEach(deferredAggregates::add);
        return aggregates;
    }

    private List<PolymorphicAggregateManifest> resolvePolymorphicAggregates(
            PrefabContext context,
            RoundEnvironment environment
    ) {
        var polymorphicAggregateElements = Stream.concat(
                        environment.getElementsAnnotatedWith(Aggregate.class).stream()
                                .filter(e -> e.getModifiers().contains(Modifier.SEALED))
                                .map(TypeElement.class::cast),
                        deferredPolymorphicAggregates.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var polymorphicAggregates = polymorphicAggregateElements.stream()
                .filter(element -> element.getPermittedSubclasses().stream()
                        .filter(s -> s.getKind() == TypeKind.DECLARED)
                        .map(s -> (TypeElement) ((DeclaredType) s).asElement())
                        .noneMatch(ClassManifest::hasUnresolvedFields))
                .flatMap(element -> guarded(context, element, () -> PolymorphicAggregateManifest.of(element, processingEnv)))
                .toList();

        deferredPolymorphicAggregates.clear();
        polymorphicAggregateElements.stream()
                .filter(element -> element.getPermittedSubclasses().stream()
                        .filter(s -> s.getKind() == TypeKind.DECLARED)
                        .map(s -> (TypeElement) ((DeclaredType) s).asElement())
                        .anyMatch(ClassManifest::hasUnresolvedFields))
                .forEach(deferredPolymorphicAggregates::add);
        return polymorphicAggregates;
    }

    /**
     * Evaluates a supplier behind the failure boundary; a failure is reported and yields an empty stream so that
     * independent aggregates are still processed.
     */
    private <T> Stream<T> guarded(PrefabContext context, TypeElement element, Supplier<T> supplier) {
        var result = new ArrayList<T>(1);
        context.guard().run(PROCESSOR_COMPONENT, element, () -> result.add(supplier.get()));
        return result.stream();
    }

    private List<ClassManifest> writeAggregates(PrefabContext context, List<ClassManifest> aggregates) {
        var testClientWriter = createTestClientWriter(context);
        var written = new ArrayList<ClassManifest>();
        aggregates.forEach(manifest -> {
            if (!processedAggregateNames.add(manifest.qualifiedName())) {
                // Already handled in an earlier round: only keep it unless that attempt failed.
                if (!failedAggregateNames.contains(manifest.qualifiedName())) {
                    written.add(manifest);
                }
                return;
            }
            var succeeded = context.guard().run(PROCESSOR_COMPONENT, manifest.type().asElement(),
                    () -> GeneratedFiles.buffered(processingEnv, () -> {
                        new HttpWriter(context).writeHttpLayer(manifest);
                        new ApplicationWriter(context).writeApplicationLayer(manifest);
                        new PersistenceWriter(context).writePersistenceLayer(manifest);
                        testClientWriter.writeTestSupport(manifest);
                    }));
            if (succeeded) {
                written.add(manifest);
            } else {
                failedAggregateNames.add(manifest.qualifiedName());
            }
        });
        return written;
    }

    private List<PolymorphicAggregateManifest> writePolymorphicAggregates(
            PrefabContext context,
            List<PolymorphicAggregateManifest> polymorphicAggregates
    ) {
        var testClientWriter = createTestClientWriter(context);
        var written = new ArrayList<PolymorphicAggregateManifest>();
        polymorphicAggregates.forEach(manifest -> {
            var name = manifest.packageName() + "." + manifest.simpleName();
            if (!processedPolymorphicAggregateNames.add(name)) {
                if (!failedAggregateNames.contains(name)) {
                    written.add(manifest);
                }
                return;
            }
            var succeeded = context.guard().run(PROCESSOR_COMPONENT, manifest.type().asElement(),
                    () -> GeneratedFiles.buffered(processingEnv, () -> {
                        new PersistenceWriter(context).writePolymorphicPersistenceLayer(manifest);
                        new PolymorphicJdbcConverterWriter(context).writeConverters(manifest);
                        new HttpWriter(context).writePolymorphicHttpLayer(manifest);
                        new ApplicationWriter(context).writePolymorphicApplicationLayer(manifest);
                        testClientWriter.writePolymorphicTestSupport(manifest);
                    }));
            if (succeeded) {
                written.add(manifest);
            } else {
                failedAggregateNames.add(name);
            }
        });
        return written;
    }

    protected TestClientWriter createTestClientWriter(PrefabContext context) {
        return new TestClientWriter(context);
    }

    private void writeAdditionalFilesIfNeeded(
            PrefabContext context,
            List<PrefabPlugin> plugins,
            List<ClassManifest> aggregates,
            List<PolymorphicAggregateManifest> polymorphicAggregates) {
        var newAggregates = aggregates.stream()
                .filter(m -> !processedAdditionalFilesAggregates.contains(m.qualifiedName()))
                .toList();
        var newPolymorphicAggregates = polymorphicAggregates.stream()
                .filter(m -> !processedAdditionalFilesPolymorphicAggregates.contains(
                        m.packageName() + "." + m.simpleName()))
                .toList();
        var eventScope = plugins.stream()
                .map(PrefabPlugin::additionalFileEventScope)
                .max(Enum::compareTo)
                .orElse(PrefabContext.EventScope.CURRENT_COMPILATION);
        var currentEventElementNames = context.eventElements(eventScope)
                .map(e -> e.getQualifiedName().toString())
                .collect(Collectors.toSet());
        var hasNewEventElements = !seenEventElementNames.containsAll(currentEventElementNames);
        if (newAggregates.isEmpty() && newPolymorphicAggregates.isEmpty() && !hasNewEventElements) {
            return;
        }
        plugins.forEach(plugin -> writeAdditionalFilesForPlugin(context, plugin, newAggregates, newPolymorphicAggregates));
        newAggregates.stream().map(ClassManifest::qualifiedName).forEach(processedAdditionalFilesAggregates::add);
        newPolymorphicAggregates.stream()
                .map(m -> m.packageName() + "." + m.simpleName())
                .forEach(processedAdditionalFilesPolymorphicAggregates::add);
        seenEventElementNames.addAll(currentEventElementNames);
    }

    private void writeGlobalFiles(PrefabContext context, List<PrefabPlugin> plugins) {
        plugins.forEach(plugin -> {
                    var globallyEnabled = context.configuration().isPluginEnabled(plugin.getClass());
                    var defaultAggregates = allResolvedAggregates.stream()
                            .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                            .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.DEFAULT)
                            .toList();
                    var mainAggregates = allResolvedAggregates.stream()
                            .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                            .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.MAIN)
                            .toList();
                    var testAggregates = allResolvedAggregates.stream()
                            .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                            .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.TEST)
                            .toList();
                    var defaultPolymorphic = allResolvedPolymorphicAggregates.stream()
                            .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                            .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.DEFAULT)
                            .toList();
                    var mainPolymorphic = allResolvedPolymorphicAggregates.stream()
                            .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                            .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.MAIN)
                            .toList();
                    var testPolymorphic = allResolvedPolymorphicAggregates.stream()
                            .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                            .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.TEST)
                            .toList();
                    if (!globallyEnabled && defaultAggregates.isEmpty() && mainAggregates.isEmpty() && testAggregates.isEmpty()
                            && defaultPolymorphic.isEmpty() && mainPolymorphic.isEmpty() && testPolymorphic.isEmpty()) {
                        return;
                    }
                    if (defaultAggregates.isEmpty() && mainAggregates.isEmpty() && testAggregates.isEmpty()
                            && defaultPolymorphic.isEmpty() && mainPolymorphic.isEmpty() && testPolymorphic.isEmpty()) {
                        runPlugin(context, plugin, p -> p.writeGlobalFiles(List.of(), List.of()));
                        return;
                    }
                    if (!defaultAggregates.isEmpty() || !defaultPolymorphic.isEmpty()) {
                        runPlugin(context, plugin, p -> p.writeGlobalFiles(defaultAggregates, defaultPolymorphic));
                    }
                    if (!mainAggregates.isEmpty() || !mainPolymorphic.isEmpty()) {
                        context.withOutputTarget(OutputTarget.MAIN,
                                () -> runPlugin(context, plugin, p -> p.writeGlobalFiles(mainAggregates, mainPolymorphic)));
                    }
                    if (!testAggregates.isEmpty() || !testPolymorphic.isEmpty()) {
                        context.withOutputTarget(OutputTarget.TEST,
                                () -> runPlugin(context, plugin, p -> p.writeGlobalFiles(testAggregates, testPolymorphic)));
                    }
                });
    }

    private void writeAdditionalFilesForPlugin(
            PrefabContext context,
            PrefabPlugin plugin,
            List<ClassManifest> aggregates,
            List<PolymorphicAggregateManifest> polymorphicAggregates) {
        var globallyEnabled = context.configuration().isPluginEnabled(plugin.getClass());
        var defaultAggregates = aggregates.stream()
                .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.DEFAULT)
                .toList();
        var mainAggregates = aggregates.stream()
                .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.MAIN)
                .toList();
        var testAggregates = aggregates.stream()
                .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.TEST)
                .toList();
        var defaultPolymorphic = polymorphicAggregates.stream()
                .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.DEFAULT)
                .toList();
        var mainPolymorphic = polymorphicAggregates.stream()
                .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.MAIN)
                .toList();
        var testPolymorphic = polymorphicAggregates.stream()
                .filter(manifest -> context.isPluginEnabledFor(manifest.type().asElement(), plugin.getClass()))
                .filter(manifest -> context.getOutputTargetFor(manifest.type().asElement(), plugin.getClass()) == OutputTarget.TEST)
                .toList();
        if (!globallyEnabled && defaultAggregates.isEmpty() && mainAggregates.isEmpty() && testAggregates.isEmpty()
                && defaultPolymorphic.isEmpty() && mainPolymorphic.isEmpty() && testPolymorphic.isEmpty()) {
            return;
        }
        if (defaultAggregates.isEmpty() && mainAggregates.isEmpty() && testAggregates.isEmpty()
                && defaultPolymorphic.isEmpty() && mainPolymorphic.isEmpty() && testPolymorphic.isEmpty()) {
            runPlugin(context, plugin, p -> p.writeAdditionalFiles(List.of(), List.of()));
            return;
        }
        if (!defaultAggregates.isEmpty() || !defaultPolymorphic.isEmpty()) {
            runPlugin(context, plugin, p -> p.writeAdditionalFiles(defaultAggregates, defaultPolymorphic));
        }
        if (!mainAggregates.isEmpty() || !mainPolymorphic.isEmpty()) {
            context.withOutputTarget(OutputTarget.MAIN,
                    () -> runPlugin(context, plugin, p -> p.writeAdditionalFiles(mainAggregates, mainPolymorphic)));
        }
        if (!testAggregates.isEmpty() || !testPolymorphic.isEmpty()) {
            context.withOutputTarget(OutputTarget.TEST,
                    () -> runPlugin(context, plugin, p -> p.writeAdditionalFiles(testAggregates, testPolymorphic)));
        }
    }

    private List<PrefabPlugin> detectPlugins() {
        if (cachedPlugins == null) {
            var loader = ServiceLoader.load(PrefabPlugin.class, getClass().getClassLoader()).iterator();
            cachedPlugins = sorted(StreamSupport.stream(Spliterators.spliteratorUnknownSize(loader, 0), false));
        }
        return cachedPlugins;
    }

    private static List<PrefabPlugin> sorted(Stream<PrefabPlugin> plugins) {
        return plugins.sorted(PrefabPlugin.EXECUTION_ORDER).toList();
    }
}
