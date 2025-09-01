package be.appify.prefab.processor.search;

import be.appify.prefab.core.annotations.rest.Search;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.TypeSpec;

import java.util.Optional;

public class SearchPlugin implements PrefabPlugin {
    private final SearchControllerWriter searchControllerWriter = new SearchControllerWriter();
    private final SearchServiceWriter searchServiceWriter = new SearchServiceWriter();
    private final SearchRepositoryWriter searchRepositoryWriter = new SearchRepositoryWriter();
    private final SearchRepositoryAdapterWriter searchRepositoryAdapterWriter = new SearchRepositoryAdapterWriter();
    private final SearchCrudRepositoryWriter searchCrudRepositoryWriter = new SearchCrudRepositoryWriter();
    private final SearchTestFixtureWriter searchTestFixtureWriter = new SearchTestFixtureWriter();

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        searchAnnotation(manifest).ifPresent(search ->
                builder.addMethod(searchControllerWriter.searchMethod(manifest, search)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        searchAnnotation(manifest).ifPresent(search ->
                builder.addMethod(searchServiceWriter.searchMethod(manifest, getSearchProperty(manifest, search))));
    }

    @Override
    public void writeRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        searchAnnotation(manifest).ifPresent(search ->
                builder.addMethod(searchRepositoryWriter.searchMethod(manifest, getSearchProperty(manifest, search))));
    }

    @Override
    public void writeRepositoryAdapter(ClassManifest manifest, TypeSpec.Builder builder) {
        searchAnnotation(manifest).ifPresent(search ->
                builder.addMethod(
                        searchRepositoryAdapterWriter.searchMethod(manifest, getSearchProperty(manifest, search))));
    }

    @Override
    public void writeCrudRepository(ClassManifest manifest, TypeSpec.Builder builder) {
        searchAnnotation(manifest)
                .flatMap(search -> searchCrudRepositoryWriter.searchMethod(manifest,
                        getSearchProperty(manifest, search)))
                .ifPresent(builder::addMethod);
    }

    @Override
    public void writeTestFixture(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        searchAnnotation(manifest)
                .map(search -> searchTestFixtureWriter.searchMethod(manifest))
                .ifPresent(builder::addMethod);
    }

    private Optional<Search> searchAnnotation(ClassManifest manifest) {
        return manifest.annotationsOfType(Search.class)
                .stream()
                .findFirst();
    }

    private VariableManifest getSearchProperty(ClassManifest manifest, Search search) {
        if (search.property().isBlank()) {
            return null;
        }
        return manifest.fieldByName(search.property())
                .map(variable -> {
                    if (variable.type().is(Reference.class)) {
                        return variable.withType(String.class);
                    }
                    return variable;
                })
                .orElseThrow(() -> new IllegalArgumentException("Property %s.%s defined in Http.Search not found"
                        .formatted(manifest.qualifiedName(), search.property())));
    }
}
