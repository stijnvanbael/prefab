package be.appify.prefab.processor.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CreatePlugin implements PrefabPlugin {
    private final CreateControllerWriter controllerWriter = new CreateControllerWriter();
    private final CreateServiceWriter serviceWriter = new CreateServiceWriter();
    private final CreateRequestRecordWriter requestRecordWriter = new CreateRequestRecordWriter();
    private final CreateTestFixtureWriter testFixtureWriter = new CreateTestFixtureWriter();

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        createConstructorOf(manifest).ifPresent(createConstructor ->
                builder.addMethod(controllerWriter.createMethod(manifest, createConstructor, context)));
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest, PrefabContext context) {
        return createConstructorOf(classManifest)
                .stream()
                .flatMap(constructor -> constructor.getParameters()
                        .stream()
                        .map(param ->
                                new VariableManifest(param, context.processingEnvironment())))
                .filter(param -> param.type().is(Reference.class))
                .map(param -> {
                    var type = param.type().parameters().getFirst();
                    return ClassName.get("%s.application".formatted(type.packageName()),
                            "%sRepository".formatted(type.simpleName()));
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        createConstructorOf(manifest).ifPresent(createConstructor ->
                builder.addMethod(
                        serviceWriter.createMethod(manifest, createConstructor, context.requestParameterMapper())));
    }

    @Override
    public void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        createConstructorOf(manifest).ifPresent(createConstructor ->
                testFixtureWriter.createMethods(manifest, createConstructor, context).forEach(builder::addMethod));
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest -> createConstructorOf(manifest).ifPresent(createConstructor -> {
                if (!createConstructor.getParameters().isEmpty()) {
                    requestRecordWriter.writeRequestRecord(fileWriter, manifest, createConstructor, context);
                }
            }));
        }
    }

    private Optional<ExecutableElement> createConstructorOf(ClassManifest manifest) {
        var createConstructors = manifest.constructorsWith(Create.class);
        if (createConstructors.isEmpty()) {
            return Optional.empty();
        }
        if (createConstructors.size() > 1) {
            throw new IllegalStateException(
                    "Multiple constructors with @Create annotation found in " + manifest.qualifiedName());
        }
        return createConstructors.stream().findFirst();
    }
}
