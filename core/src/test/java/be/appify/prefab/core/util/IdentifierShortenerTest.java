package be.appify.prefab.core.util;

import org.junit.jupiter.api.Test;

import static be.appify.prefab.core.util.IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH;
import static be.appify.prefab.core.util.IdentifierShortener.columnName;
import static be.appify.prefab.core.util.IdentifierShortener.foreignKeyConstraintName;
import static be.appify.prefab.core.util.IdentifierShortener.indexName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierShortenerTest {

    @Test
    void shortIdentifierIsKeptAsIs() {
        assertEquals("customer_name", columnName("customerName"));
    }

    @Test
    void longColumnNameIsShortenedToPostgresLimit() {
        var result = columnName("thisIsAnExceptionallyVerboseNestedPropertyNameForDemonstratingColumnShortening");

        assertTrue(result.length() <= POSTGRES_MAX_IDENTIFIER_LENGTH);
    }

    @Test
    void indexSuffixIsPreservedWhenNameIsShortened() {
        var result = indexName(
                "this_is_a_very_long_table_name_that_pushes_postgres_identifier_limits",
                "this_is_a_very_long_column_name_that_is_also_close_to_the_limit",
                false
        );

        assertTrue(result.length() <= POSTGRES_MAX_IDENTIFIER_LENGTH);
        assertTrue(result.endsWith("_ix"));
    }

    @Test
    void foreignKeySuffixIsPreservedWhenNameIsShortened() {
        var result = foreignKeyConstraintName(
                "this_is_a_very_long_table_name_that_pushes_postgres_identifier_limits",
                "this_is_a_very_long_column_name_that_is_also_close_to_the_limit"
        );

        assertTrue(result.length() <= POSTGRES_MAX_IDENTIFIER_LENGTH);
        assertTrue(result.endsWith("_fk"));
    }

    @Test
    void duplicateSegmentsAreRemovedBeforeShortening() {
        assertEquals("person_name_first", columnName("personNameFirstName"));
    }

    @Test
    void deduplicationOccursEvenWhenBelowMaxLength() {
        var result = columnName("personNameFirstName");

        assertTrue(result.length() < POSTGRES_MAX_IDENTIFIER_LENGTH);
        assertEquals("person_name_first", result);
    }

    @Test
    void deduplicationPreservesFirstOccurrenceOfEachSegment() {
        assertEquals("order_line_total_price", columnName("orderLineTotalPriceLinePrice"));
    }
}

