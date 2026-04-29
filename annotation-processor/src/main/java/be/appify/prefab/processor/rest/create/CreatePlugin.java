package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;

/**
 * Plugin that handles the @Create annotation to generate controller methods, service methods, request records, and test client methods for
 * creating new instances of a class.
 */
public class CreatePlugin implements PrefabPlugin {
    private final CreateControllerWriter controllerWriter = new CreateControllerWriter();
    private final AsyncCreateControllerWriter asyncControllerWriter = new AsyncCreateControllerWriter();
    private final CreateServiceWriter serviceWriter = new CreateServiceWriter();
    private final AsyncCreateServiceWriter asyncServiceWriter = new AsyncCreateServiceWriter();
    private final CreateRequestRecordWriter requestRecordWriter = new CreateRequestRecordWriter();
    private final CreateTestClientWriter testClientWriter = new CreateTestClientWriter();
    private final Map<ClassManifest, Optional<ExecutableElement>> createConstructorsCache =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<ClassManifest, Optional<ExecutableElement>> asyncCreateMethodsCache =
            Collections.synchronizedMap(new WeakHashMap<>());
    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        asyncCreateFactoryOf(manifest).ifPresent(factory ->
                builder.addMethod(asyncControllerWriter.createMethod(manifest, factory, context)));
        if (asyncCreateFactoryOf(manifest).isEmpty()) {
            createConstructorOf(manifest).ifPresent(createConstructor ->
                    builder.addMethod(controllerWriter.createMethod(manifest, createConstructor, context)));
        }
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        asyncCreateFactoryOf(manifest).ifPresent(factory ->
                builder.addMethod(asyncServiceWriter.createMethod(manifest, factory, context)));
        if (asyncCreateFactoryOf(manifest).isEmpty()) {
            createConstructorOf(manifest).ifPresent(createConstructor ->
                    builder.addMethod(serviceWriter.createMethod(manifest, createConstructor, context)));
        }
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        if (asyncCreateFactoryOf(manifest).isEmpty()) {
            createConstructorOf(manifest).ifPresent(createConstructor ->
                    testClientWriter.createMethods(manifest, createConstructor, context).forEach(builder::addMethod));
        }
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest -> {
                asyncCreateFactoryOf(manifest).ifPresent(factory -> {
                    if (!factory.getParameters().isEmpty()) {
                        requestRecordWriter.writeRequestRecordForFactory(fileWriter, manifest, factory, context);
                    }
                });
                if (asyncCreateFactoryOf(manifest).isEmpty()) {
                    createConstructorOf(manifest).ifPresent(createConstructor -> {
                        if (!createConstructor.getParameters().isEmpty()) {
                            requestRecordWriter.writeRequestRecord(fileWriter, manifest, createConstructor, context);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        writeAdditionalFiles(manifests);
        if (!polymorphicManifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            polymorphicManifests.forEach(polymorphic -> writePolymorphicAdditionalFiles(fileWriter, polymorphic));
        }
    }

    private void writePolymorphicAdditionalFiles(JavaFileWriter fileWriter, PolymorphicAggregateManifest polymorphic) {
        var grouped = groupSubtypesByPath(polymorphic);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                requestRecordWriter.writeUnionRequestInterface(fileWriter, polymorphic, entries, context);
            } else {
                entries.forEach(e -> {
                    if (!e.getValue().getParameters().isEmpty()) {
                        requestRecordWriter.writeRequestRecordForPolymorphic(
                                fileWriter, polymorphic, e.getKey(), e.getValue(), context);
                    }
                });
            }
        });
    }

    @Override
    public Set<TypeName> getPolymorphicServiceDependencies(PolymorphicAggregateManifest manifest) {
        return manifest.parent()
                .filter(parent -> !parent.type().parameters().isEmpty())
                .flatMap(parent -> manifest.subtypes().stream()
                        .flatMap(subtype -> createConstructorOf(subtype).stream())
                        .flatMap(ctor -> ctor.getParameters().stream())
                        .filter(p -> parent.name().equals(p.getSimpleName().toString()))
                        .filter(p -> ((javax.lang.model.type.DeclaredType) p.asType()).getTypeArguments().isEmpty())
                        .findFirst()
                        .map(p -> {
                            var parentType = parent.type().parameters().getFirst();
                            return (TypeName) ClassName.get(parentType.packageName() + ".application",
                                    parentType.simpleName() + "Repository");
                        }))
                .map(Set::of)
                .orElse(Set.of());
    }

    @Override
    public void writePolymorphicController(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        var grouped = groupSubtypesByPath(manifest);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                builder.addMethod(controllerWriter.createDispatchMethodForPolymorphic(manifest, entries, context));
            } else {
                entries.forEach(e -> builder.addMethod(
                        controllerWriter.createMethodForPolymorphic(manifest, e.getKey(), e.getValue(), context)));
            }
        });
    }

    @Override
    public void writePolymorphicService(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        var grouped = groupSubtypesByPath(manifest);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                entries.forEach(e -> createConstructorOf(e.getKey()).ifPresent(ctor ->
                        builder.addMethod(serviceWriter.createMethodForPolymorphicUnion(manifest, e.getKey(), ctor, context))));
            } else {
                entries.forEach(e -> createConstructorOf(e.getKey()).ifPresent(ctor ->
                        builder.addMethod(serviceWriter.createMethodForPolymorphic(manifest, e.getKey(), ctor, context))));
            }
        });
    }

    @Override
    public void writePolymorphicTestClient(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        var grouped = groupSubtypesByPath(manifest);
        grouped.forEach((pathKey, entries) -> {
            if (isUnionGroup(entries)) {
                var create = entries.getFirst().getValue().getAnnotation(Create.class);
                builder.addMethod(testClientWriter.baseCreateMethodForPolymorphic(manifest, create));
                entries.forEach(e -> testClientWriter.createMethodsForPolymorphicUnion(manifest, e, context)
                        .forEach(builder::addMethod));
            } else {
                entries.forEach(e -> testClientWriter.createMethodsForPolymorphic(manifest, e.getKey(), e.getValue(), context)
                        .forEach(builder::addMethod));
            }
        });
    }

    private Map<String, List<Map.Entry<ClassManifest, ExecutableElement>>> groupSubtypesByPath(
            PolymorphicAggregateManifest manifest
    ) {
        return manifest.subtypes().stream()
                .flatMap(subtype -> createConstructorOf(subtype)
                        .map(c -> Map.entry(subtype, c))
                        .stream())
                .collect(Collectors.groupingBy(
                        e -> {
                            var create = e.getValue().getAnnotation(Create.class);
                            return create.method() + "|" + create.path();
                        },
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static boolean isUnionGroup(List<Map.Entry<ClassManifest, ExecutableElement>> entries) {
        return entries.size() >= 2 && entries.stream().anyMatch(e -> !e.getValue().getParameters().isEmpty());
    }

    private Optional<ExecutableElement> asyncCreateFactoryOf(ClassManifest manifest) {
        return asyncCreateMethodsCache.computeIfAbsent(manifest, m -> {
            var factories = m.staticMethodsWith(Create.class).stream()
                    .filter(method -> m.isAsyncCommit()
                            || method.getAnnotationsByType(AsyncCommit.class).length > 0)
                    .toList();
            if (factories.size() > 1) {
                context.logError(
                        "Multiple async @Create static methods found in " + m.qualifiedName(),
                        factories.get(1));
            }
            return factories.stream().findFirst();
        });
    }

    private Optional<ExecutableElement> createConstructorOf(ClassManifest manifest) {
        return createConstructorsCache.computeIfAbsent(manifest, m -> {
            var createConstructors = m.constructorsWith(Create.class);
            if (createConstructors.isEmpty()) {
                return Optional.empty();
            }
            if (createConstructors.size() > 1) {
                context.logError(
                        "Multiple constructors with @Create annotation found in " + m.qualifiedName(),
                        createConstructors.get(1));
            }
            return createConstructors.stream().findFirst();
        });
    }
}
