package be.appify.prefab.processor.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

import java.util.Optional;

public class DeletePlugin implements PrefabPlugin {
    private final DeleteControllerWriter controllerWriter = new DeleteControllerWriter();
    private final DeleteServiceWriter serviceWriter = new DeleteServiceWriter();
    private final DeleteRepositoryWriter repositoryWriter = new DeleteRepositoryWriter();
    private final DeleteRepositoryAdapterWriter repositoryAdapterWriter = new DeleteRepositoryAdapterWriter();
    private final DeleteTestFixtureWriter testFixtureWriter = new DeleteTestFixtureWriter();

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        deleteAnnotation(manifest).ifPresent(delete ->
                builder.addMethod(controllerWriter.deleteMethod(delete)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        deleteAnnotation(manifest).ifPresent(ignored ->
                builder.addMethod(serviceWriter.deleteMethod(manifest)));
    }

    @Override
    public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        deleteAnnotation(manifest).ifPresent(ignored ->
                builder.addMethod(repositoryWriter.deleteMethod()));
    }

    @Override
    public void writeRepositoryAdapter(ClassManifest manifest, TypeSpec.Builder builder) {
        deleteAnnotation(manifest).ifPresent(ignored ->
                builder.addMethod(repositoryAdapterWriter.deleteMethod()));
    }

    @Override
    public void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        deleteAnnotation(manifest).ifPresent(ignored ->
                testFixtureWriter.deleteMethods(manifest).forEach(builder::addMethod));
    }

    private Optional<Delete> deleteAnnotation(ClassManifest manifest) {
        return manifest.annotationsOfType(Delete.class)
                .stream()
                .findFirst();
    }
}
