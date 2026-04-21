package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.util.IdentifierShortener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexTest {

    @Test
    void nonUniqueIndexName() {
        var index = Index.of("product", "category_id", false);
        assertEquals("product_category_id_ix", index.name());
        assertFalse(index.unique());
    }

    @Test
    void uniqueIndexName() {
        var index = Index.of("product", "sku", true);
        assertEquals("product_sku_uk", index.name());
        assertTrue(index.unique());
    }

    @Test
    void createIndexSql() {
        var index = Index.of("product", "category_id", false);
        assertEquals(
                "CREATE INDEX \"product_category_id_ix\" ON \"product\" (\"category_id\");\n",
                index.toCreateSql("product")
        );
    }

    @Test
    void createUniqueIndexSql() {
        var index = Index.of("product", "sku", true);
        assertEquals(
                "CREATE UNIQUE INDEX \"product_sku_uk\" ON \"product\" (\"sku\");\n",
                index.toCreateSql("product")
        );
    }

    @Test
    void dropIndexSql() {
        var index = Index.of("product", "category_id", false);
        assertEquals(
                "DROP INDEX \"product_category_id_ix\";\n",
                index.toDropSql()
        );
    }

    @Test
    void longIndexNameIsShortenedToPostgresLimit() {
        var index = Index.of(
                "this_is_a_very_long_table_name_that_pushes_postgres_identifier_limits",
                "this_is_a_very_long_column_name_that_is_also_close_to_the_limit",
                false
        );

        assertTrue(index.name().length() <= IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH);
        assertTrue(index.name().endsWith("_ix"));
    }
}
