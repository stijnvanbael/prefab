package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AutocompleteRepositoryWriter")
class AutocompleteRepositoryWriterTest {

    // -------------------------------------------------------------------------
    // JDBC — ScanMode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("JDBC PREFIX EXACT generates LIKE CONCAT(:query, '%')")
    void jdbcPrefixExact() {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("brand", "product", ScanMode.PREFIX, MatchStrategy.EXACT)
                .toString();

        assertTrue(source.contains("\\\"brand\\\" LIKE CONCAT(:query, '%')"));
        assertFalse(source.contains("LOWER"));
        assertTrue(source.contains("autocompleteByBrand("));
    }

    @Test
    @DisplayName("JDBC PREFIX IGNORE_CASE generates LOWER(col) LIKE LOWER(CONCAT(:query, '%'))")
    void jdbcPrefixIgnoreCase() {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("brand", "product", ScanMode.PREFIX, MatchStrategy.IGNORE_CASE)
                .toString();

        assertTrue(source.contains("LOWER(\\\"brand\\\") LIKE LOWER(CONCAT(:query, '%'))"));
    }

    @Test
    @DisplayName("JDBC PREFIX FUZZY uses <% operator — only matches prefix-positioned similar words")
    void jdbcPrefixFuzzy() {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("brand", "product", ScanMode.PREFIX, MatchStrategy.FUZZY)
                .toString();

        assertTrue(source.contains("LOWER(:query) <% LOWER(\\\"brand\\\")"));
        assertFalse(source.contains("LIKE"));
        assertFalse(source.replace("word_similarity", "").contains("similarity("));
    }

    @Test
    @DisplayName("JDBC CONTAINS EXACT generates LIKE CONCAT('%', :query, '%')")
    void jdbcContainsExact() {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("brand", "product", ScanMode.CONTAINS, MatchStrategy.EXACT)
                .toString();

        assertTrue(source.contains("\\\"brand\\\" LIKE CONCAT('%', :query, '%')"));
        assertFalse(source.contains("LOWER"));
    }

    @Test
    @DisplayName("JDBC CONTAINS IGNORE_CASE generates LOWER(col) LIKE LOWER(CONCAT('%', :query, '%'))")
    void jdbcContainsIgnoreCase() {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("name", "product", ScanMode.CONTAINS, MatchStrategy.IGNORE_CASE)
                .toString();

        assertTrue(source.contains("LOWER(\\\"name\\\") LIKE LOWER(CONCAT('%', :query, '%'))"));
        assertTrue(source.contains("SELECT DISTINCT \\\"name\\\" FROM \\\"product\\\""));
        assertTrue(source.contains("ORDER BY \\\"name\\\""));
        assertTrue(source.contains("autocompleteByName("));
    }

    @Test
    @DisplayName("JDBC CONTAINS FUZZY uses full-string similarity for whole-value fuzzy matching")
    void jdbcContainsFuzzy() {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("brand", "product", ScanMode.CONTAINS, MatchStrategy.FUZZY)
                .toString();

        assertTrue(source.contains("similarity(LOWER(\\\"brand\\\"), LOWER(:query)) > 0.3"));
        assertFalse(source.contains("word_similarity"));
        assertFalse(source.contains("LIKE"));
    }

    // -------------------------------------------------------------------------
    // MongoDB — ScanMode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Mongo PREFIX EXACT generates anchored regex without options")
    void mongoPrefixExact() {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("name", ScanMode.PREFIX, MatchStrategy.EXACT)
                .toString();

        assertTrue(source.contains("'$regex': '^?0'"));
        assertFalse(source.contains("$options"));
        assertTrue(source.contains("autocompleteByName("));
    }

    @Test
    @DisplayName("Mongo PREFIX IGNORE_CASE generates anchored regex with 'i' option")
    void mongoPrefixIgnoreCase() {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("name", ScanMode.PREFIX, MatchStrategy.IGNORE_CASE)
                .toString();

        assertTrue(source.contains("'$regex': '^?0'"));
        assertTrue(source.contains("'$options': 'i'"));
    }

    @Test
    @DisplayName("Mongo PREFIX FUZZY falls back to anchored case-insensitive regex")
    void mongoPrefixFuzzy() {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("name", ScanMode.PREFIX, MatchStrategy.FUZZY)
                .toString();

        assertTrue(source.contains("'$regex': '^?0'"));
        assertTrue(source.contains("'$options': 'i'"));
    }

    @Test
    @DisplayName("Mongo CONTAINS EXACT generates unanchored regex without options")
    void mongoContainsExact() {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("name", ScanMode.CONTAINS, MatchStrategy.EXACT)
                .toString();

        assertTrue(source.contains("'$regex': '?0'"));
        assertFalse(source.contains("$options"));
        assertFalse(source.contains("^?0"));
    }

    @Test
    @DisplayName("Mongo CONTAINS IGNORE_CASE generates unanchored regex with 'i' option")
    void mongoContainsIgnoreCase() {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("name", ScanMode.CONTAINS, MatchStrategy.IGNORE_CASE)
                .toString();

        assertTrue(source.contains("'$regex': '?0'"));
        assertTrue(source.contains("'$options': 'i'"));
        assertTrue(source.contains("'$group': { '_id': '$name' }"));
        assertTrue(source.contains("'$sort': { '_id': 1 }"));
    }

    @Test
    @DisplayName("Mongo CONTAINS FUZZY falls back to unanchored case-insensitive regex")
    void mongoContainsFuzzy() {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("name", ScanMode.CONTAINS, MatchStrategy.FUZZY)
                .toString();

        assertTrue(source.contains("'$regex': '?0'"));
        assertTrue(source.contains("'$options': 'i'"));
    }

    // -------------------------------------------------------------------------
    // Common structure assertions
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(MatchStrategy.class)
    @DisplayName("JDBC method always includes pageable parameter and ORDER BY")
    void jdbcAlwaysHasPageableAndOrderBy(MatchStrategy strategy) {
        var source = AutocompleteRepositoryWriter
                .autocompleteJdbcMethod("field", "table", ScanMode.PREFIX, strategy)
                .toString();

        assertTrue(source.contains("Pageable pageable"));
        assertTrue(source.contains("ORDER BY"));
    }

    @ParameterizedTest
    @EnumSource(MatchStrategy.class)
    @DisplayName("Mongo method always includes group and sort aggregation stages")
    void mongoAlwaysHasGroupAndSort(MatchStrategy strategy) {
        var source = AutocompleteRepositoryWriter
                .autocompleteMongoMethod("field", ScanMode.CONTAINS, strategy)
                .toString();

        assertTrue(source.contains("'$group'"));
        assertTrue(source.contains("'$sort'"));
    }
}
