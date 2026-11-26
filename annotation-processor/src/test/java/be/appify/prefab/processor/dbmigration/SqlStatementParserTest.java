package be.appify.prefab.processor.dbmigration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlStatementParserTest {

    @Test
    void inlineForeignKeyIsRecordedInColumnState() {
        var tables = new HashMap<String, Table>();
        var sql = """
                CREATE TABLE "customer_order_history_entry" (
                  "customer_order_history" VARCHAR (255) NOT NULL REFERENCES "customer_order_history"("id"),
                  "order" VARCHAR (255) NOT NULL
                );
                """;
        new SqlStatementParser().parse(sql, tables);

        var table = tables.get("customer_order_history_entry");
        var foreignKey = table.getColumn("customer_order_history")
                .orElseThrow()
                .foreignKey();
        assertEquals(new ForeignKeyReference("customer_order_history", "id"), foreignKey);
    }

    @Test
    void dropTableStatementRemovesTableFromReplayedState() {
        var tables = new HashMap<String, Table>();
        tables.put("chapter", tableWithForeignKey());
        var sql = "DROP TABLE chapter;";

        new SqlStatementParser().parse(sql, tables);

        assertFalse(tables.containsKey("chapter"));
    }

    @Test
    void createTableFollowedByDropTableLeavesNoTrace() {
        var tables = new HashMap<String, Table>();
        var sql = """
                CREATE TABLE chapter (id VARCHAR(255) PRIMARY KEY);
                DROP TABLE chapter;
                """;

        new SqlStatementParser().parse(sql, tables);

        assertTrue(tables.isEmpty());
    }

    @Test
    void dropConstraintStatementDoesNotCrash() {
        var tables = new HashMap<String, Table>();
        tables.put("chapter", tableWithForeignKey());
        var sql = "ALTER TABLE chapter DROP CONSTRAINT chapter_course_fk;";

        new SqlStatementParser().parse(sql, tables);

        var column = tables.get("chapter").getColumn("course").orElseThrow();
        assertNull(column.foreignKey());
    }

    @Test
    void dropConstraintFollowedByOtherStatementsParsesAll() {
        var tables = new HashMap<String, Table>();
        tables.put("chapter", tableWithForeignKey());
        var sql = """
                ALTER TABLE chapter DROP CONSTRAINT chapter_course_fk;
                ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk FOREIGN KEY (course) REFERENCES course(id);
                """;

        new SqlStatementParser().parse(sql, tables);

        var column = tables.get("chapter").getColumn("course").orElseThrow();
        assertNotNull(column.foreignKey());
    }

    @Test
    void droppingNonExistingColumnFailsWithMigrationDiagnostic() {
        var tables = new HashMap<String, Table>();
        tables.put("chapter", tableWithForeignKey());

        var error = assertThrows(MigrationSqlException.class,
                () -> new SqlStatementParser().parse("ALTER TABLE chapter DROP COLUMN missing;", tables));

        assertTrue(error.getMessage().contains("Cannot drop non-existing column: missing"));
    }

    @Test
    void dropColumnIfExistsAllowsAbsentColumn() {
        var tables = new HashMap<String, Table>();
        tables.put("chapter", tableWithForeignKey());

        new SqlStatementParser().parse("ALTER TABLE chapter DROP COLUMN IF EXISTS missing;", tables);

        assertTrue(tables.get("chapter").getColumn("missing").isEmpty());
    }

    private Table tableWithForeignKey() {
        return new Table(
                "chapter",
                List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null),
                        new Column("course", new DataType.Varchar(255), false,
                                new ForeignKeyReference("course", "id"), null)
                ),
                List.of("id")
        );
    }
}
