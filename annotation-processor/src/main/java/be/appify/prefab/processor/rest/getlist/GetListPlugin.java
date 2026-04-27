package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;

/**
 * Prefab plugin that generates getList controller, service, repository, and test client methods based on the @GetList
 * annotation.
 */
public class GetListPlugin implements PrefabPlugin {
    private final GetListControllerWriter controllerWriter = new GetListControllerWriter();
    private final GetListServiceWriter serviceWriter = new GetListServiceWriter();
    private final GetListRepositoryWriter repositoryWriter = new GetListRepositoryWriter();
    private final GetListTestClientWriter testClientWriter = new GetListTestClientWriter();


    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest).ifPresent(getList ->
                builder.addMethod(controllerWriter.getListMethod(manifest, getList)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest).ifPresent(getList ->
                builder.addMethod(serviceWriter.getListMethod(manifest)));
    }

    @Override
    public void writePolymorphicService(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.annotationsOfType(GetList.class).stream().findFirst().ifPresent(ignored ->
                builder.addMethod(serviceWriter.getListMethod(manifest)));
    }

    @Override
    public void writePolymorphicController(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.annotationsOfType(GetList.class).stream().findFirst().ifPresent(getList ->
                builder.addMethod(controllerWriter.getListMethod(manifest, getList)));
    }

    @Override
    public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest)
                .flatMap(getList -> repositoryWriter.getListMethod(manifest))
                .ifPresent(builder::addMethod);
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest)
                .map(getList -> testClientWriter.getListMethod(manifest))
                .ifPresent(builder::addMethod);
    }

    @Override
    public void writePolymorphicTestClient(PolymorphicAggregateManifest manifest, TypeSpec.Builder builder) {
        manifest.annotationsOfType(GetList.class).stream().findFirst()
                .map(ignored -> testClientWriter.getListMethodForPolymorphic(manifest))
                .ifPresent(builder::addMethod);
    }

    private Optional<GetList> getListAnnotation(ClassManifest manifest) {
        return manifest.annotationsOfType(GetList.class)
                .stream()
                .findFirst();
    }
}
