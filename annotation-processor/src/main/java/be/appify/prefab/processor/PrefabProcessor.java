package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Aggregate;
import com.google.auto.service.AutoService;
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
import javax.annotation.processing.SupportedSourceVersion;
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
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
@SuppressWarnings("unused")
public class PrefabProcessor extends AbstractProcessor {

    // Aggregates whose field types were unresolved in a previous round.
    // AbstractProcessor is instantiated once per compilation, so instance state persists across rounds.
    private final Set<TypeElement> deferredAggregates = new LinkedHashSet<>();
    private final Set<TypeElement> deferredPolymorphicAggregates = new LinkedHashSet<>();
    private final Set<ExecutableElement> deferredEventHandlers = new LinkedHashSet<>();

    /** Constructs a new PrefabProcessor. */
    public PrefabProcessor() {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        var plugins = detectPlugins();

        // Merge freshly discovered aggregates with any that were deferred from a previous round
        // because their field types were not yet generated.
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

        // Keep elements that are still unresolved for the next round
        deferredAggregates.clear();
        aggregateElements.stream()
                .filter(ClassManifest::hasUnresolvedFields)
                .forEach(deferredAggregates::add);

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

        var context = new PrefabContext(processingEnv, plugins, environment, deferredEventHandlers);
        plugins.forEach(plugin -> plugin.initContext(context));
        aggregates.forEach(manifest -> {
            new HttpWriter(context).writeHttpLayer(manifest);
            new ApplicationWriter(context).writeApplicationLayer(manifest);
            new PersistenceWriter(context).writePersistenceLayer(manifest);
            new TestClientWriter(context).writeTestSupport(manifest);
        });
        polymorphicAggregates.forEach(manifest -> {
            new PersistenceWriter(context).writePolymorphicPersistenceLayer(manifest);
            new PolymorphicJdbcConverterWriter(context).writeConverters(manifest);
            new HttpWriter(context).writePolymorphicHttpLayer(manifest);
            new ApplicationWriter(context).writePolymorphicApplicationLayer(manifest);
        });
        plugins.forEach(plugin -> plugin.writeAdditionalFiles(aggregates, polymorphicAggregates));
        deferredEventHandlers.clear();
        deferredEventHandlers.addAll(context.newlyDeferredEventHandlers());
        return true;
    }

    private List<PrefabPlugin> detectPlugins() {
        var loader = ServiceLoader.load(PrefabPlugin.class, getClass().getClassLoader()).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(loader, 0), false)
                .toList();
    }
}
