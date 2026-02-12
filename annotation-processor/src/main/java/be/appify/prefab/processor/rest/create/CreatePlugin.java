package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.spring.ReferenceFactory;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import javax.lang.model.element.ExecutableElement;

/**
 * Plugin that handles the @Create annotation to generate controller methods, service methods, request records, and test client methods for
 * creating new instances of a class.
 */
public class CreatePlugin implements PrefabPlugin {
    private CreateControllerWriter controllerWriter;
    private CreateServiceWriter serviceWriter;
    private CreateRequestRecordWriter requestRecordWriter;
    private CreateTestClientWriter testClientWriter;
    private final Map<ClassManifest, Optional<ExecutableElement>> createConstructorsCache =
            Collections.synchronizedMap(new WeakHashMap<>());
    private PrefabContext context;

    /** Creates a new instance of the CreatePlugin. */
    public CreatePlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        controllerWriter = new CreateControllerWriter(context);
        serviceWriter = new CreateServiceWriter(context);
        requestRecordWriter = new CreateRequestRecordWriter(context);
        testClientWriter = new CreateTestClientWriter(context);
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        createConstructorOf(manifest).ifPresent(createConstructor ->
                builder.addMethod(controllerWriter.createMethod(manifest, createConstructor)));
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest) {
        return createConstructorOf(classManifest)
                .stream()
                .flatMap(constructor -> constructor.getParameters()
                        .stream()
                        .map(param ->
                                VariableManifest.of(param, context.processingEnvironment())))
                .anyMatch(param -> param.type().is(Reference.class))
                ? Set.of(ClassName.get(ReferenceFactory.class)) : Collections.emptySet();
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        createConstructorOf(manifest).ifPresent(createConstructor ->
                builder.addMethod(
                        serviceWriter.createMethod(manifest, createConstructor)));
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        createConstructorOf(manifest).ifPresent(createConstructor ->
                testClientWriter.createMethods(manifest, createConstructor).forEach(builder::addMethod));
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest -> createConstructorOf(manifest).ifPresent(createConstructor -> {
                if (!createConstructor.getParameters().isEmpty()) {
                    requestRecordWriter.writeRequestRecord(fileWriter, manifest, createConstructor);
                }
            }));
        }
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
