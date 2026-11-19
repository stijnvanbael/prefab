package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.Exposure;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.rest.ExposureResolver;
import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.TypeElement;

/**
 * Prefab plugin that contributes repository autocomplete methods for fields annotated with @Autocomplete.
 */
public class AutocompletePlugin implements PrefabPlugin {

    private final AutocompleteControllerWriter controllerWriter = new AutocompleteControllerWriter();
    private final AutocompleteServiceWriter serviceWriter = new AutocompleteServiceWriter();
    private final AutocompleteRepositoryWriter repositoryWriter = new AutocompleteRepositoryWriter();
    private final AutocompleteTestClientWriter testClientWriter = new AutocompleteTestClientWriter();

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        if (hasPublicAggregateExposure(manifest)) {
            controllerWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
        }
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        serviceWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
    }

    @Override
    public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        repositoryWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        if (hasPublicAggregateExposure(manifest)) {
            testClientWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
        }
    }

    private static boolean hasPublicAggregateExposure(ClassManifest manifest) {
        return ExposureResolver.isPublic((TypeElement) manifest.element(), Exposure.INHERIT);
    }
}
