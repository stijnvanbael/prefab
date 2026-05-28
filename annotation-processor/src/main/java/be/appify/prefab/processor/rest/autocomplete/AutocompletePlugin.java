package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

/**
 * Prefab plugin that contributes repository autocomplete methods for fields annotated with @Autocomplete.
 */
public class AutocompletePlugin implements PrefabPlugin {

    private final AutocompleteRepositoryWriter repositoryWriter = new AutocompleteRepositoryWriter();

    @Override
    public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        repositoryWriter.autocompleteMethods(manifest).forEach(builder::addMethod);
    }
}

