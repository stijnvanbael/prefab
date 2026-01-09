package be.appify.prefab.processor.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import java.util.Objects;
import java.util.Optional;

/**
 * Prefab plugin that generates delete controller, service, and test client methods based on the @Delete annotation.
 */
public class DeletePlugin implements PrefabPlugin {
    private final DeleteControllerWriter controllerWriter = new DeleteControllerWriter();
    private final DeleteServiceWriter serviceWriter = new DeleteServiceWriter();
    private final DeleteTestClientWriter testClientWriter = new DeleteTestClientWriter();

    /** Creates a new instance of DeletePlugin. */
    public DeletePlugin() {
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        var typeDelete = typeDelete(manifest);
        typeDelete.ifPresent(delete ->
                builder.addMethod(controllerWriter.deleteMethod(delete)));
        var deleteMethod = deleteMethod(manifest, context);
        if (typeDelete.isPresent() && deleteMethod.isPresent()) {
            context.logError("Delete annotation is present on both type and method. Please choose one.",
                    deleteMethod.get());
        } else {
            deleteMethod.ifPresent(method ->
                    builder.addMethod(controllerWriter.deleteMethod(
                            Objects.requireNonNull(method.getAnnotation(Delete.class)))));
        }
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        typeDelete(manifest).ifPresentOrElse(ignored ->
                        builder.addMethod(serviceWriter.deleteMethod(manifest)),
                () -> deleteMethod(manifest, context).ifPresent(method ->
                        builder.addMethod(serviceWriter.deleteMethod(manifest, method))));
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        typeDelete(manifest).ifPresentOrElse(ignored ->
                        testClientWriter.deleteMethods(manifest).forEach(builder::addMethod),
                () -> deleteMethod(manifest, context).ifPresent(method ->
                        testClientWriter.deleteMethods(manifest).forEach(builder::addMethod)));
    }

    private Optional<Delete> typeDelete(ClassManifest manifest) {
        return manifest.annotationsOfType(Delete.class)
                .stream()
                .findFirst();
    }

    private Optional<ExecutableElement> deleteMethod(ClassManifest manifest, PrefabContext context) {
        var deleteMethods = manifest.methodsWith(Delete.class)
                .stream()
                .toList();
        if (deleteMethods.size() > 1) {
            context.logError("Only one delete method is allowed per aggregate root: " +
                    manifest.className(), deleteMethods.get(1));
        }
        return deleteMethods.stream()
                .peek(method -> {
                    if (!method.getParameters().isEmpty()) {
                        context.logError("Delete method should not have any parameters: " +
                                manifest.className(), method);
                    }
                })
                .findFirst();
    }
}
