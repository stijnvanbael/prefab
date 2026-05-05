package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Avsc;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

/**
 * Annotation processor for Prefab framework that generates code based on annotated aggregates.
 */
@SupportedAnnotationTypes({
        "be.appify.prefab.core.annotations.*",
})
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

    // Tracking sets for deduplication across rounds (source elements are visible in every round).
    private final Set<String> processedAggregateNames = new LinkedHashSet<>();
    private final Set<String> processedPolymorphicAggregateNames = new LinkedHashSet<>();
    private final Set<String> processedAdditionalFilesAggregates = new LinkedHashSet<>();
    private final Set<String> processedAdditionalFilesPolymorphicAggregates = new LinkedHashSet<>();
    private final Set<String> seenEventElementNames = new LinkedHashSet<>();
    private final Set<String> currentCompilationTypeNames = new LinkedHashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        var plugins = detectPlugins();
        rememberCurrentCompilationTypeNames(environment);
        var context = new PrefabContext(
                processingEnv,
                plugins,
                environment,
                deferredEventHandlers,
                currentCompilationTypeNames);
        plugins.forEach(plugin -> plugin.initContext(context));
        if (hasAvscAnnotations(context) && !eventFilesWritten) {
            eventFilesWritten = true;
            plugins.forEach(PrefabPlugin::writeEventFiles);
        }
        var aggregates = resolveAggregates(environment);
        var polymorphicAggregates = resolvePolymorphicAggregates(environment);
        writeAggregates(context, aggregates);
        writePolymorphicAggregates(context, polymorphicAggregates);
        writeAdditionalFilesIfNeeded(context, plugins, aggregates, polymorphicAggregates);
        allResolvedAggregates.addAll(aggregates);
        allResolvedPolymorphicAggregates.addAll(polymorphicAggregates);
        if (!globalFilesWritten && deferredAggregates.isEmpty() && deferredPolymorphicAggregates.isEmpty()) {
            globalFilesWritten = true;
            plugins.forEach(plugin -> plugin.writeGlobalFiles(allResolvedAggregates, allResolvedPolymorphicAggregates));
        }
        deferredEventHandlers.clear();
        deferredEventHandlers.addAll(context.newlyDeferredEventHandlers());
        return true;
    }

    private void rememberCurrentCompilationTypeNames(RoundEnvironment environment) {
        environment.getRootElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .map(type -> type.getQualifiedName().toString())
                .forEach(currentCompilationTypeNames::add);
    }

    private boolean hasAvscAnnotations(PrefabContext context) {
        return context.avscElementsFromCurrentCompilation().findAny().isPresent();
    }

    private List<ClassManifest> resolveAggregates(RoundEnvironment environment) {
        var aggregateElements = Stream.concat(
                        environment.getElementsAnnotatedWith(Aggregate.class).stream()
                                .filter(e -> e.getKind().isClass() && !e.getModifiers().contains(Modifier.ABSTRACT))
                                .map(TypeElement.class::cast),
                        deferredAggregates.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var aggregates = aggregateElements.stream()
                .filter(element -> !ClassManifest.hasUnresolvedFields(element))
                .map(element -> ClassManifest.of(element, processingEnv))
                .toList();

        deferredAggregates.clear();
        aggregateElements.stream()
                .filter(ClassManifest::hasUnresolvedFields)
                .forEach(deferredAggregates::add);
        return aggregates;
    }

    private List<PolymorphicAggregateManifest> resolvePolymorphicAggregates(RoundEnvironment environment) {
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
                .map(element -> PolymorphicAggregateManifest.of(element, processingEnv))
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

    private void writeAggregates(PrefabContext context, List<ClassManifest> aggregates) {
        var testClientWriter = createTestClientWriter(context);
        aggregates.stream()
                .filter(manifest -> processedAggregateNames.add(manifest.qualifiedName()))
                .forEach(manifest -> {
                    new HttpWriter(context).writeHttpLayer(manifest);
                    new ApplicationWriter(context).writeApplicationLayer(manifest);
                    new PersistenceWriter(context).writePersistenceLayer(manifest);
                    testClientWriter.writeTestSupport(manifest);
                });
    }

    private void writePolymorphicAggregates(PrefabContext context, List<PolymorphicAggregateManifest> polymorphicAggregates) {
        var testClientWriter = createTestClientWriter(context);
        polymorphicAggregates.stream()
                .filter(manifest -> processedPolymorphicAggregateNames.add(
                        manifest.packageName() + "." + manifest.simpleName()))
                .forEach(manifest -> {
                    new PersistenceWriter(context).writePolymorphicPersistenceLayer(manifest);
                    new PolymorphicJdbcConverterWriter(context).writeConverters(manifest);
                    new HttpWriter(context).writePolymorphicHttpLayer(manifest);
                    new ApplicationWriter(context).writePolymorphicApplicationLayer(manifest);
                    testClientWriter.writePolymorphicTestSupport(manifest);
                });
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
        var currentEventElementNames = context.eventElements()
                .map(e -> e.getQualifiedName().toString())
                .collect(Collectors.toSet());
        var hasNewEventElements = !seenEventElementNames.containsAll(currentEventElementNames);
        if (newAggregates.isEmpty() && newPolymorphicAggregates.isEmpty() && !hasNewEventElements) {
            return;
        }
        plugins.forEach(plugin -> plugin.writeAdditionalFiles(newAggregates, newPolymorphicAggregates));
        newAggregates.stream().map(ClassManifest::qualifiedName).forEach(processedAdditionalFilesAggregates::add);
        newPolymorphicAggregates.stream()
                .map(m -> m.packageName() + "." + m.simpleName())
                .forEach(processedAdditionalFilesPolymorphicAggregates::add);
        seenEventElementNames.addAll(currentEventElementNames);
    }

    private List<PrefabPlugin> detectPlugins() {
        var loader = ServiceLoader.load(PrefabPlugin.class, getClass().getClassLoader()).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(loader, 0), false)
                .toList();
    }
}
