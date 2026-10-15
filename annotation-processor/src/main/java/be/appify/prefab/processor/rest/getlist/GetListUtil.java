package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class GetListUtil {
    private static final Set<String> RANGE_COMPARABLE_TYPES = Set.of(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
            "java.lang.Double", "java.math.BigDecimal", "java.math.BigInteger", "java.time.Instant",
            "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime", "java.time.OffsetDateTime",
            "java.time.ZonedDateTime");

    static List<FilterManifest> filterPropertiesOf(ClassManifest manifest) {
        return manifest.fields().stream()
                .filter(field -> field.hasAnnotation(Filter.class) || field.hasAnnotation(Filters.class))
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
                .map(filter -> FilterManifest.of(field, filter.operator(), filter.ignoreCase()));
    }

    private static Stream<FilterManifest> singleFilter(VariableManifest field) {
        return field.getAnnotation(Filter.class)
                .map(AnnotationManifest::value)
                .map(filter ->
                        FilterManifest.of(field, filter.operator(), filter.ignoreCase()))
                .stream();
    }

    record FilterManifest(VariableManifest field, Filter.Operator operator, boolean ignoreCase) {

        static FilterManifest of(VariableManifest field, Filter.Operator operator, boolean ignoreCase) {
            var exposedField = !RangeCondition.isRange(operator) && field.type().isSingleValueType()
                    ? field.withType(String.class)
                    : field;
            return new FilterManifest(exposedField, operator, ignoreCase);
        }

        boolean isRange() {
            return RangeCondition.isRange(operator);
        }

        String parameterName() {
            return field.name() + switch (operator) {
                case GREATER_THAN -> "GreaterThan";
                case GREATER_THAN_OR_EQUAL -> "GreaterThanOrEqual";
                case LESS_THAN -> "LessThan";
                case LESS_THAN_OR_EQUAL -> "LessThanOrEqual";
                default -> "";
            };
        }
    }

    static boolean isRangeComparable(TypeManifest type) {
        var boxed = type.asBoxed();
        return RANGE_COMPARABLE_TYPES.contains(boxed.packageName() + "." + boxed.simpleName());
    }

    static List<FilterManifest> exampleFiltersOf(List<FilterManifest> filters) {
        return filters.stream().filter(filter -> !filter.isRange()).toList();
    }

    static List<FilterManifest> rangeFiltersOf(List<FilterManifest> filters) {
        return filters.stream().filter(FilterManifest::isRange).toList();
    }
}
