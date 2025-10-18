package be.appify.prefab.processor.update;

import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.spring.ReferenceFactory;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UpdatePlugin implements PrefabPlugin {
    private final UpdateControllerWriter updateControllerWriter = new UpdateControllerWriter();
    private final UpdateServiceWriter updateServiceWriter = new UpdateServiceWriter();
    private final UpdateRequestRecordWriter updateRequestRecordWriter = new UpdateRequestRecordWriter();
    private final UpdateTestFixtureWriter updateTestFixtureWriter = new UpdateTestFixtureWriter();

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        updateMethodsOf(manifest, context).forEach(update ->
                builder.addMethod(updateControllerWriter.updateMethod(manifest, update, context)));
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest, PrefabContext context) {
        return updateMethodsOf(classManifest, context).stream()
                .flatMap(method -> method.parameters().stream())
                .anyMatch(param -> param.type().is(Reference.class))
                ? Set.of(ClassName.get(ReferenceFactory.class)) : Collections.emptySet();
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        updateMethodsOf(manifest, context).forEach(update ->
                builder.addMethod(updateServiceWriter.updateMethod(manifest, update)));
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest -> updateMethodsOf(manifest, context).forEach(update -> {
                if (!update.parameters().isEmpty()) {
                    updateRequestRecordWriter.writeUpdateRequestRecord(fileWriter, manifest, update,
                            context.requestParameterBuilder());
                }
            }));
        }
    }

    @Override
    public void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        updateMethodsOf(manifest, context).forEach(update ->
                builder.addMethod(updateTestFixtureWriter.updateMethod(manifest, update, context)));
    }

    private List<UpdateManifest> updateMethodsOf(ClassManifest manifest, PrefabContext context) {
        return manifest.methodsWith(Update.class).stream()
                .map(element -> {
                    var update = element.getAnnotationsByType(Update.class)[0];
                    return new UpdateManifest(
                            element.getSimpleName().toString(),
                            getParametersOf(element, context.processingEnvironment()),
                            element.getReturnType().toString().equals("void"),
                            update.method(),
                            update.path());
                })
                .toList();
    }

    private List<VariableManifest> getParametersOf(Element createConstructor,
            ProcessingEnvironment processingEnvironment) {
        return ((ExecutableElement) createConstructor).getParameters()
                .stream()
                .map(element -> new VariableManifest(element, processingEnvironment))
                .toList();
    }
}
