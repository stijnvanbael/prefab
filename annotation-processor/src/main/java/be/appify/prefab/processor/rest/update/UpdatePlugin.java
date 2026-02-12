package be.appify.prefab.processor.rest.update;

import be.appify.prefab.core.annotations.rest.Update;
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
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/**
 * Plugin responsible for generating update controller and service methods, as well as related request records and test
 * client methods.
 */
public class UpdatePlugin implements PrefabPlugin {
    private final UpdateControllerWriter updateControllerWriter = new UpdateControllerWriter();
    private final UpdateServiceWriter updateServiceWriter = new UpdateServiceWriter();
    private final UpdateRequestRecordWriter updateRequestRecordWriter = new UpdateRequestRecordWriter();
    private final UpdateTestClientWriter updateTestClientWriter = new UpdateTestClientWriter();
    private PrefabContext context;

    /** Constructs a new UpdatePlugin. */
    public UpdatePlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        updateMethodsOf(manifest).forEach(update ->
                builder.addMethod(updateControllerWriter.updateMethod(manifest, update, context)));
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest) {
        return updateMethodsOf(classManifest).stream()
                .flatMap(method -> method.parameters().stream())
                .anyMatch(param -> param.type().is(Reference.class))
                ? Set.of(ClassName.get(ReferenceFactory.class)) : Collections.emptySet();
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        updateMethodsOf(manifest).forEach(update ->
                builder.addMethod(updateServiceWriter.updateMethod(manifest, update)));
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!manifests.isEmpty()) {
            var fileWriter = new JavaFileWriter(context.processingEnvironment(), "application");
            manifests.forEach(manifest -> updateMethodsOf(manifest).forEach(update -> {
                if (!update.parameters().isEmpty()) {
                    updateRequestRecordWriter.writeUpdateRequestRecord(fileWriter, manifest, update,
                            context.requestParameterBuilder());
                }
            }));
        }
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        updateMethodsOf(manifest).forEach(update ->
                builder.addMethod(updateTestClientWriter.updateMethod(manifest, update, context)));
    }

    private List<UpdateManifest> updateMethodsOf(ClassManifest manifest) {
        return manifest.methodsWith(Update.class).stream()
                .map(element -> {
                    var update = element.getAnnotationsByType(Update.class)[0];
                    return new UpdateManifest(
                            element.getSimpleName().toString(),
                            getParametersOf(element, context.processingEnvironment()),
                            element.getReturnType().toString().equals("void"),
                            update.method(),
                            update.path(),
                            update.security());
                })
                .toList();
    }

    private List<VariableManifest> getParametersOf(Element createConstructor,
            ProcessingEnvironment processingEnvironment) {
        return ((ExecutableElement) createConstructor).getParameters()
                .stream()
                .map(element -> VariableManifest.of(element, processingEnvironment))
                .toList();
    }
}
