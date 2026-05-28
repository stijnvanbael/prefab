package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

/**
 * Prefab plugin that contributes repository autocomplete methods for fields annotated with @Autocomplete.
 */
public class AutocompletePlugin implements PrefabPlugin {

    private final AutocompleteControllerWriter controllerWriter = new AutocompleteControllerWriter();
    private final AutocompleteServiceWriter serviceWriter = new AutocompleteServiceWriter();
    private final AutocompleteRepositoryWriter repositoryWriter = new AutocompleteRepositoryWriter();

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        controllerWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        serviceWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
    }

    @Override
    public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        repositoryWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
    }
}

