package be.appify.prefab.processor.dbmigration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexTest {

    @Test
    void nonUniqueIndexName() {
        var index = Index.of("product", "category_id", false);
        assertEquals("product_category_id_idx", index.name());
        assertFalse(index.unique());
    }

    @Test
    void uniqueIndexName() {
        var index = Index.of("product", "sku", true);
        assertEquals("product_sku_uidx", index.name());
        assertTrue(index.unique());
    }

    @Test
    void createIndexSql() {
        var index = Index.of("product", "category_id", false);
        assertEquals(
                "CREATE INDEX \"product_category_id_idx\" ON \"product\" (\"category_id\");\n",
                index.toCreateSql("product")
        );
    }

    @Test
    void createUniqueIndexSql() {
        var index = Index.of("product", "sku", true);
        assertEquals(
                "CREATE UNIQUE INDEX \"product_sku_uidx\" ON \"product\" (\"sku\");\n",
                index.toCreateSql("product")
        );
    }

    @Test
    void dropIndexSql() {
        var index = Index.of("product", "category_id", false);
        assertEquals(
                "DROP INDEX \"product_category_id_idx\";\n",
                index.toDropSql()
        );
    }
}
