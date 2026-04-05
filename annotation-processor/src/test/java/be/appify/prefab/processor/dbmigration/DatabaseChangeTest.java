package be.appify.prefab.processor.dbmigration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseChangeTest {

    @Test
    void createIndexSql() {
        var index = Index.of("product", "category_id", false);
        var change = new DatabaseChange.CreateIndex("product", index);
        assertEquals(
                "CREATE INDEX \"product_category_id_idx\" ON \"product\" (\"category_id\");\n",
                change.toSql()
        );
    }

    @Test
    void createUniqueIndexSql() {
        var index = Index.of("product", "sku", true);
        var change = new DatabaseChange.CreateIndex("product", index);
        assertEquals(
                "CREATE UNIQUE INDEX \"product_sku_uidx\" ON \"product\" (\"sku\");\n",
                change.toSql()
        );
    }

    @Test
    void dropIndexSql() {
        var index = Index.of("product", "category_id", false);
        var change = new DatabaseChange.DropIndex(index);
        assertEquals(
                "DROP INDEX \"product_category_id_idx\";\n",
                change.toSql()
        );
    }

    @Test
    void createTableWithIndexesSql() {
        var table = new Table(
                "product",
                List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null),
                        new Column("name", new DataType.Varchar(255), false, null, null)
                ),
                List.of("id"),
                List.of(Index.of("product", "name", false))
        );
        var change = new DatabaseChange.CreateTable(table);
        // Indexes are generated separately from CREATE TABLE, so the SQL should not include them
        assertEquals(
                "CREATE TABLE \"product\" (\n" +
                        "  \"id\" VARCHAR (255) NOT NULL,\n" +
                        "  \"name\" VARCHAR (255) NOT NULL,\n" +
                        "  PRIMARY KEY(\"id\")\n" +
                        ");\n",
                change.toSql()
        );
    }
}
