package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.core.annotations.rest.Filter.Weight;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exposes the fields that take part in the full-text filter of an aggregate, for plugins that need to create the
 * matching indexes.
 */
public final class FullTextFields {
    private FullTextFields() {
    }

    /**
     * Names of the fields of the aggregate that are searched by its FULL_TEXT filter, in declaration order.
     *
     * @param manifest
     *         the aggregate
     * @return the field names, empty when the aggregate has no FULL_TEXT filter
     */
    public static List<String> namesOf(ClassManifest manifest) {
        return GetListUtil.fullTextFiltersOf(GetListUtil.declaredFiltersOf(manifest)).stream()
                .flatMap(filter -> filter.searchedProperties().stream())
                .toList();
    }

    /**
     * The relevance weight of each field that is searched by the FULL_TEXT filter, in declaration order.
     *
     * @param manifest
     *         the aggregate
     * @return the weight by field name, empty when the aggregate has no FULL_TEXT filter
     */
    public static Map<String, Weight> weightsOf(ClassManifest manifest) {
        var weights = new LinkedHashMap<String, Weight>();
        GetListUtil.fullTextFiltersOf(GetListUtil.declaredFiltersOf(manifest))
                .forEach(filter -> weights.putAll(filter.weights()));
        return weights;
    }

    /**
     * The language of the FULL_TEXT filter, when one of its fields declares it.
     *
     * @param manifest
     *         the aggregate
     * @return the language, empty when the application-wide default applies
     */
    public static Optional<String> languageOf(ClassManifest manifest) {
        return GetListUtil.fullTextFiltersOf(GetListUtil.declaredFiltersOf(manifest)).stream()
                .map(GetListUtil.FilterManifest::language)
                .filter(language -> !language.isBlank())
                .findFirst();
    }

    /**
     * Names of the fields that are searched as type-ahead, which need an index on the initials of their words.
     *
     * @param manifest
     *         the aggregate
     * @return the field names, empty when the aggregate has no type-ahead FULL_TEXT filter
     */
    public static List<String> typeaheadNamesOf(ClassManifest manifest) {
        var filters = GetListUtil.fullTextFiltersOf(GetListUtil.declaredFiltersOf(manifest));
        return filters.stream().anyMatch(GetListUtil.FilterManifest::typeahead) ? namesOf(manifest) : List.of();
    }

    /**
     * Names of the fields that are searched fuzzily, which need a trigram index.
     *
     * @param manifest
     *         the aggregate
     * @return the field names, empty when the aggregate has no fuzzy FULL_TEXT filter
     */
    public static List<String> fuzzyNamesOf(ClassManifest manifest) {
        var filters = GetListUtil.fullTextFiltersOf(GetListUtil.declaredFiltersOf(manifest));
        return GetListUtil.isFuzzy(filters) ? namesOf(manifest) : List.of();
    }

    /**
     * Whether the field is only filtered with FULL_TEXT, in which case a regular index on the field is useless.
     *
     * @param field
     *         the field
     * @return true if the field has filters and all of them are FULL_TEXT
     */
    public static boolean isOnlyFullText(VariableManifest field) {
        var filters = GetListUtil.filtersOf(field).toList();
        return !filters.isEmpty() && filters.stream().allMatch(GetListUtil.FilterManifest::isFullText);
    }
}