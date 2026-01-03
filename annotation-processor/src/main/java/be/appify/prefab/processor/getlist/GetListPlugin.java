package be.appify.prefab.processor.getlist;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

import java.util.Optional;

/**
 * Prefab plugin that generates getList controller, service, repository, and test fixture methods based on the @GetList annotation.
 */
public class GetListPlugin implements PrefabPlugin {
    private final GetListControllerWriter controllerWriter = new GetListControllerWriter();
    private final GetListServiceWriter serviceWriter = new GetListServiceWriter();
    private final GetListRepositoryWriter repositoryWriter = new GetListRepositoryWriter();
    private final GetListTestFixtureWriter testFixtureWriter = new GetListTestFixtureWriter();

    /** Creates a new instance of GetListPlugin. */
    public GetListPlugin() {
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        getListAnnotation(manifest).ifPresent(getList ->
                builder.addMethod(controllerWriter.getListMethod(manifest, getList)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        getListAnnotation(manifest).ifPresent(getList ->
                builder.addMethod(serviceWriter.getListMethod(manifest)));
    }

    @Override
    public void writeCrudRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest)
                .flatMap(getList -> repositoryWriter.getListMethod(manifest))
                .ifPresent(builder::addMethod);
    }

    @Override
    public void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        getListAnnotation(manifest)
                .map(getList -> testFixtureWriter.getListMethod(manifest))
                .ifPresent(builder::addMethod);
    }

    private Optional<GetList> getListAnnotation(ClassManifest manifest) {
        return manifest.annotationsOfType(GetList.class)
                .stream()
                .findFirst();
    }
}
