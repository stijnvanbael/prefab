package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import be.appify.prefab.core.util.FullTextLanguages;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.RestOperationPlugin;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static be.appify.prefab.processor.rest.getlist.GetListUtil.declaredFiltersOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.filterPropertiesOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.fullTextFiltersOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.rangeFiltersOf;

/**
 * Prefab plugin that generates getList controller, service, repository, and test client methods based on the @GetList
 * annotation.
 */
public class GetListPlugin extends RestOperationPlugin {
    private final GetListControllerWriter controllerWriter = new GetListControllerWriter();
    private final GetListServiceWriter serviceWriter = new GetListServiceWriter();
    private final GetListQueryWriter queryWriter = new GetListQueryWriter();
    private final GetListRepositoryWriter repositoryWriter = new GetListRepositoryWriter();
    private final GetListTestClientWriter testClientWriter = new GetListTestClientWriter();


    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest).ifPresent(getList -> {
            if (defaultSortIsValid(manifest, getList)) {
                builder.addMethod(controllerWriter.getListMethod(manifest, getList));
            }
        });
    }

    private boolean defaultSortIsValid(ClassManifest manifest, GetList getList) {
        var fieldNames = manifest.fields().stream().map(VariableManifest::name).collect(Collectors.toSet());
        var unknown = Stream.of(getList.sorted())
                .map(SortOrder::property)
                .filter(field -> !fieldNames.contains(field))
                .toList();
        unknown.forEach(field -> context().logError(
                "[@GetList] sorted: unknown property '%s' on %s".formatted(field, manifest.simpleName()),
                manifest.element()));
        return unknown.isEmpty();
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest manifest) {
        if (getListAnnotation(manifest).isEmpty()) {
            return Set.of();
        }
        var filters = filterPropertiesOf(manifest);
        if (!fullTextFiltersOf(filters).isEmpty()) {
            return Set.of(ClassName.get(FullTextQueryExecutor.class));
        }
        return rangeFiltersOf(filters).isEmpty() ? Set.of() : Set.of(ClassName.get(RangeQueryExecutor.class));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest).ifPresent(getList -> {
            if (filtersAreValid(manifest)) {
                builder.addMethod(serviceWriter.getListMethod(manifest));
                builder.addMethod(serviceWriter.getListByQueryMethod(manifest, getList));
            }
        });
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        var queryManifests = manifests.stream()
                .filter(manifest -> getListAnnotation(manifest).isPresent())
                .filter(this::filtersAreValid)
                .toList();
        if (!queryManifests.isEmpty()) {
            var fileWriter = new OutputTargetFileOutput(context(), "application", OutputTarget.MAIN);
            queryManifests.forEach(manifest -> queryWriter.writeQuery(fileWriter, manifest));
        }
    }

    private boolean filtersAreValid(ClassManifest manifest) {
        return rangeFiltersAreValid(manifest) & fullTextFiltersAreValid(manifest);
    }

    private boolean fullTextFiltersAreValid(ClassManifest manifest) {
        var declared = declaredFiltersOf(manifest);
        var fullText = fullTextFiltersOf(declared);
        var searched = fullText.stream().map(filter -> filter.searchedProperties().getFirst()).toList();
        var nonString = manifest.fields().stream()
                .filter(field -> searched.contains(field.name()) && !field.type().is(String.class))
                .toList();
        nonString.forEach(field -> context().logError(
                "[@Filter] %s: operator FULL_TEXT is only supported on String fields".formatted(field.name()),
                field.element()));
        var parameters = fullText.stream().map(GetListUtil.FilterManifest::parameterName).distinct().toList();
        var tooMany = parameters.size() > 1;
        if (tooMany) {
            context().logError(
                    "[@Filter] %s: only one FULL_TEXT request parameter is supported per aggregate, but found %s"
                            .formatted(manifest.simpleName(), parameters),
                    manifest.element());
        }
        var clashes = parameters.stream()
                .filter(parameter -> declared.stream().filter(filter -> !filter.isFullText())
                        .anyMatch(filter -> filter.parameterName().equals(parameter)))
                .toList();
        clashes.forEach(parameter -> context().logError(
                "[@Filter] %s: FULL_TEXT parameter '%s' clashes with another filter parameter"
                        .formatted(manifest.simpleName(), parameter),
                manifest.element()));
        var invalidThreshold = fullText.stream()
                .filter(filter -> filter.fuzzy() && (filter.similarityThreshold() <= 0 || filter.similarityThreshold() > 1))
                .toList();
        invalidThreshold.forEach(filter -> context().logError(
                "[@Filter] %s: similarityThreshold must be between 0 (exclusive) and 1"
                        .formatted(filter.searchedProperties().getFirst()),
                manifest.element()));
        var consistentFuzziness = fuzzinessIsConsistent(manifest, fullText);
        var consistentTypeahead = typeaheadIsConsistent(manifest, fullText);
        var validLanguage = languageIsValid(manifest, fullText);
        return nonString.isEmpty() && !tooMany && clashes.isEmpty() && invalidThreshold.isEmpty()
                && consistentFuzziness && consistentTypeahead && validLanguage;
    }

    private boolean languageIsValid(ClassManifest manifest, List<GetListUtil.FilterManifest> fullText) {
        if (fullText.isEmpty()) {
            return true;
        }
        var languages = fullText.stream().map(GetListUtil.FilterManifest::language).distinct().toList();
        if (languages.size() > 1) {
            context().logError(
                    "[@Filter] %s: all FULL_TEXT fields sharing a parameter must use the same language"
                            .formatted(manifest.simpleName()),
                    manifest.element());
        }
        var unknown = Stream.concat(languages.stream().filter(language -> !language.isBlank()),
                        Stream.of(context().fullTextLanguage()))
                .filter(language -> !FullTextLanguages.isKnown(language))
                .distinct()
                .toList();
        unknown.forEach(language -> context().logError(
                "[@Filter] %s: unknown full-text language '%s'; use the name of a built-in PostgreSQL text search configuration"
                        .formatted(manifest.simpleName(), language),
                manifest.element()));
        return languages.size() <= 1 && unknown.isEmpty();
    }

    private boolean typeaheadIsConsistent(ClassManifest manifest, List<GetListUtil.FilterManifest> fullText) {
        var consistent = fullText.stream().map(GetListUtil.FilterManifest::typeahead).distinct().count() <= 1;
        if (!consistent) {
            context().logError(
                    "[@Filter] %s: all FULL_TEXT fields sharing a parameter must use the same typeahead setting"
                            .formatted(manifest.simpleName()),
                    manifest.element());
        }
        return consistent;
    }

    // Fuzziness applies to the whole search, so fields sharing a parameter cannot disagree about it
    private boolean fuzzinessIsConsistent(ClassManifest manifest, List<GetListUtil.FilterManifest> fullText) {
        var settings = fullText.stream()
                .map(filter -> filter.fuzzy() ? filter.similarityThreshold() : null)
                .distinct()
                .toList();
        if (settings.size() > 1) {
            context().logError(
                    "[@Filter] %s: all FULL_TEXT fields sharing a parameter must use the same fuzzy and similarityThreshold settings"
                            .formatted(manifest.simpleName()),
                    manifest.element());
        }
        return settings.size() <= 1;
    }

    private boolean rangeFiltersAreValid(ClassManifest manifest) {
        var invalid = rangeFiltersOf(filterPropertiesOf(manifest)).stream()
                .filter(filter -> !GetListUtil.isRangeComparable(filter.field().type()))
                .toList();
        invalid.forEach(filter -> context().logError(
                "[@Filter] %s: operator %s is only supported on numeric and java.time fields, but the field type is %s"
                        .formatted(filter.field().name(), filter.operator(), filter.field().type().simpleName()),
                filter.field().element()));
        return invalid.isEmpty();
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
