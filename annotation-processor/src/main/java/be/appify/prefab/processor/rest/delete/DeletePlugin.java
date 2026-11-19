package be.appify.prefab.processor.rest.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.rest.RestOperationPlugin;
import be.appify.prefab.processor.rest.ExposureResolver;
import com.palantir.javapoet.TypeSpec;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Prefab plugin that generates delete controller, service, and test client methods based on the @Delete annotation.
 */
public class DeletePlugin extends RestOperationPlugin {
    private final DeleteControllerWriter controllerWriter = new DeleteControllerWriter();
    private final DeleteServiceWriter serviceWriter = new DeleteServiceWriter();
    private final DeleteTestClientWriter testClientWriter = new DeleteTestClientWriter();

    /** Creates a new instance of DeletePlugin. */
    public DeletePlugin() {
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        var context = context();
        var typeDelete = typeDelete(manifest);
        typeDelete.filter(delete -> ExposureResolver.isPublic(
                        (TypeElement) manifest.element(), delete.exposure()))
                .ifPresent(delete -> builder.addMethod(controllerWriter.deleteMethod(manifest, delete)));
        var deleteMethod = deleteMethod(manifest, context);
        if (typeDelete.isPresent() && deleteMethod.isPresent()) {
            context.logError("[@Delete] " + deleteMethod.get().getSimpleName() + ": @Delete cannot be present on both the class and method.\n"
                    + "Suggested fix: place @Delete on either the class or the method, not both.",
                    deleteMethod.get());
        } else {
            deleteMethod.ifPresent(method -> {
                var delete = Objects.requireNonNull(method.getAnnotation(Delete.class));
                if (ExposureResolver.isPublic((TypeElement) manifest.element(), delete.exposure())) {
                    builder.addMethod(controllerWriter.deleteMethod(manifest, delete));
                }
            });
        }
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        var context = context();
        typeDelete(manifest).ifPresentOrElse(ignored ->
                        builder.addMethod(serviceWriter.deleteMethod(manifest)),
                () -> deleteMethod(manifest, context).ifPresent(method ->
                        builder.addMethod(serviceWriter.deleteMethod(manifest, method))));
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        var context = context();
        typeDelete(manifest)
                .filter(delete -> ExposureResolver.isPublic((TypeElement) manifest.element(), delete.exposure()))
                .ifPresentOrElse(ignored ->
                        testClientWriter.deleteMethods(manifest).forEach(builder::addMethod),
                () -> deleteMethod(manifest, context)
                        .filter(method -> ExposureResolver.isPublic(
                                (TypeElement) manifest.element(), method.getAnnotation(Delete.class).exposure()))
                        .ifPresent(method -> testClientWriter.deleteMethods(manifest).forEach(builder::addMethod)));
    }

    @Override
    public void writePolymorphicController(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.annotationsOfType(Delete.class).stream().findFirst()
                .filter(delete -> ExposureResolver.isPublic(
                        (TypeElement) manifest.type().asElement(), delete.exposure()))
                .ifPresent(delete -> builder.addMethod(controllerWriter.deleteMethodForPolymorphic(manifest, delete)));
    }

    @Override
    public void writePolymorphicService(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.annotationsOfType(Delete.class).stream().findFirst().ifPresent(ignored ->
                builder.addMethod(serviceWriter.deleteMethodForPolymorphic(manifest)));
    }

    @Override
    public void writePolymorphicTestClient(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.annotationsOfType(Delete.class).stream().findFirst()
                .filter(delete -> ExposureResolver.isPublic(
                        (TypeElement) manifest.type().asElement(), delete.exposure()))
                .ifPresent(ignored -> testClientWriter.deleteMethodsForPolymorphic(manifest).forEach(builder::addMethod));
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
            context.logError("[@Delete] " + deleteMethods.get(1).getSimpleName() + ": only one @Delete method is allowed per aggregate.\n"
                    + "Suggested fix: remove @Delete from all but one method.",
                    deleteMethods.get(1));
        }
        return deleteMethods.stream()
                .peek(method -> {
                    if (!method.getParameters().isEmpty()) {
                        context.logError("[@Delete] " + method.getSimpleName() + ": @Delete method must not have parameters.\n"
                                + "Suggested fix: remove all parameters from this method.",
                                method);
                    }
                })
                .findFirst();
    }
}
