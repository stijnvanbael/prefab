package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;

import java.util.List;
import java.util.stream.Stream;

class GetListUtil {
    static List<FilterManifest> filterPropertiesOf(ClassManifest manifest) {
        return manifest.fields().stream()
                .filter(field -> field.hasAnnotation(Filter.class) || field.hasAnnotation(Filters.class))
                .map(field -> field.type().is(Reference.class) ? field.withType(String.class) : field)
                .flatMap(GetListUtil::filtersOn)
                .toList();
    }

    private static Stream<FilterManifest> filtersOn(VariableManifest field) {
        if (field.hasAnnotation(Filters.class)) {
            return multipleFilters(field);
        } else {
            return singleFilter(field);
        }
    }

    private static Stream<FilterManifest> multipleFilters(VariableManifest field) {
        return field.getAnnotation(Filters.class).stream().flatMap(filtersAnnotation ->
                        Stream.of(filtersAnnotation.value().value()))
                .map(filter -> new FilterManifest(field, filter.operator(), filter.ignoreCase()));
    }

    private static Stream<FilterManifest> singleFilter(VariableManifest field) {
        return field.getAnnotation(Filter.class)
                .map(AnnotationManifest::value)
                .map(filter ->
                        new FilterManifest(field, filter.operator(), filter.ignoreCase()))
                .stream();
    }

    record FilterManifest(VariableManifest field, Filter.Operator operator, boolean ignoreCase) {
    }
}
