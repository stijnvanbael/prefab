package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class GetListUtil {
    private static final Set<String> RANGE_COMPARABLE_TYPES = Set.of(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
            "java.lang.Double", "java.math.BigDecimal", "java.math.BigInteger", "java.time.Instant",
            "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime", "java.time.OffsetDateTime",
            "java.time.ZonedDateTime");

    static List<FilterManifest> filterPropertiesOf(ClassManifest manifest) {
        var declared = declaredFiltersOf(manifest);
        var fullText = fullTextFiltersOf(declared);
        return declared.stream()
                .filter(filter -> !filter.isFullText() || filter == fullText.getFirst())
                .map(filter -> filter.isFullText() ? FilterManifest.combined(fullText) : filter)
                .toList();
    }

    static List<FilterManifest> declaredFiltersOf(ClassManifest manifest) {
        return manifest.fields().stream()
                .filter(field -> field.hasAnnotation(Filter.class) || field.hasAnnotation(Filters.class))
                .flatMap(GetListUtil::filtersOf)
                .toList();
    }

    static boolean isFuzzy(List<FilterManifest> fullTextFilters) {
        return fullTextFilters.stream().anyMatch(FilterManifest::fuzzy);
    }

    static List<FilterManifest> fullTextFiltersOf(List<FilterManifest> filters) {
        return filters.stream().filter(FilterManifest::isFullText).toList();
    }

    static Stream<FilterManifest> filtersOf(VariableManifest field) {
        if (field.hasAnnotation(Filters.class)) {
            return multipleFilters(field);
        } else {
            return singleFilter(field);
        }
    }

    private static Stream<FilterManifest> multipleFilters(VariableManifest field) {
        return field.getAnnotation(Filters.class).stream().flatMap(filtersAnnotation ->
                        Stream.of(filtersAnnotation.value().value()))
                .map(filter -> FilterManifest.of(field, filter));
    }

    private static Stream<FilterManifest> singleFilter(VariableManifest field) {
        return field.getAnnotation(Filter.class)
                .map(AnnotationManifest::value)
                .map(filter -> FilterManifest.of(field, filter))
                .stream();
    }

    record FilterManifest(
            VariableManifest field,
            Filter.Operator operator,
            boolean ignoreCase,
            List<String> searchedProperties,
            boolean fuzzy,
            double similarityThreshold,
            Map<String, Filter.Weight> weights
    ) {

        static FilterManifest of(VariableManifest field, Filter filter) {
            if (filter.operator() == Filter.Operator.FULL_TEXT) {
                var parameter = filter.parameter().isBlank() ? field.name() : filter.parameter();
                return new FilterManifest(field.withName(parameter).withType(String.class), filter.operator(),
                        filter.ignoreCase(), List.of(field.name()), filter.fuzzy(), filter.similarityThreshold(),
                        Map.of(field.name(), filter.weight()));
            }
            var exposedField = !RangeCondition.isRange(filter.operator()) && field.type().isSingleValueType()
                    ? field.withType(String.class)
                    : field;
            return new FilterManifest(exposedField, filter.operator(), filter.ignoreCase(), List.of(), false,
                    filter.similarityThreshold(), Map.of());
        }

        // Merges the declarations of a full-text parameter into one filter that searches all declared fields
        static FilterManifest combined(List<FilterManifest> fullTextFilters) {
            var first = fullTextFilters.getFirst();
            var fuzzy = fullTextFilters.stream().filter(FilterManifest::fuzzy).findFirst();
            var weights = new LinkedHashMap<String, Filter.Weight>();
            fullTextFilters.forEach(filter -> weights.putAll(filter.weights()));
            return new FilterManifest(
                    first.field(),
                    first.operator(),
                    first.ignoreCase(),
                    fullTextFilters.stream().flatMap(filter -> filter.searchedProperties().stream()).toList(),
                    fuzzy.isPresent(),
                    fuzzy.map(FilterManifest::similarityThreshold).orElse(first.similarityThreshold()),
                    weights);
        }

        boolean isRange() {
            return RangeCondition.isRange(operator);
        }

        boolean isFullText() {
            return operator == Filter.Operator.FULL_TEXT;
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
        return filters.stream().filter(filter -> !filter.isRange() && !filter.isFullText()).toList();
    }

    static List<FilterManifest> rangeFiltersOf(List<FilterManifest> filters) {
        return filters.stream().filter(FilterManifest::isRange).toList();
    }
}
