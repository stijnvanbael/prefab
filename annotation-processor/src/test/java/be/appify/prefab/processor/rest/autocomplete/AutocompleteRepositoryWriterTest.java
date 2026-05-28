package be.appify.prefab.processor.rest.autocomplete;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AutocompleteRepositoryWriterTest {

    @Test
    void mongoAutocompleteMethodGeneratesCaseInsensitiveAggregationPipeline() {
        var method = AutocompleteRepositoryWriter.autocompleteMongoMethod("name", true);

        var source = method.toString();
        assertTrue(source.contains("$regex': '?0'"));
        assertTrue(source.contains("$options': 'i'"));
        assertTrue(source.contains("'$group': { '_id': '$name' }"));
        assertTrue(source.contains("'$sort': { '_id': 1 }"));
        assertTrue(source.contains("autocompleteByName("));
    }

    @Test
    void jdbcAutocompleteMethodGeneratesCaseSensitiveDistinctQuery() {
        var method = AutocompleteRepositoryWriter.autocompleteJdbcMethod("brand", "product", false);

        var source = method.toString();
        assertTrue(source.contains("SELECT DISTINCT \\\"brand\\\" FROM \\\"product\\\""));
        assertTrue(source.contains("\\\"brand\\\" LIKE CONCAT('%', :query, '%')"));
        assertTrue(source.contains("ORDER BY \\\"brand\\\""));
        assertTrue(source.contains("autocompleteByBrand("));
    }
}



