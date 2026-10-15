package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.RestOperationPlugin;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static be.appify.prefab.processor.rest.getlist.GetListUtil.filterPropertiesOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.rangeFiltersOf;

/**
 * Prefab plugin that generates getList controller, service, repository, and test client methods based on the @GetList
 * annotation.
 */
public class GetListPlugin extends RestOperationPlugin {
    private final GetListControllerWriter controllerWriter = new GetListControllerWriter();
    private final GetListServiceWriter serviceWriter = new GetListServiceWriter();
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
        var hasRangeFilters = getListAnnotation(manifest).isPresent()
                && !rangeFiltersOf(filterPropertiesOf(manifest)).isEmpty();
        return hasRangeFilters ? Set.of(ClassName.get(RangeQueryExecutor.class)) : Set.of();
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        getListAnnotation(manifest).ifPresent(getList -> {
            if (rangeFiltersAreValid(manifest)) {
                builder.addMethod(serviceWriter.getListMethod(manifest));
            }
        });
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
