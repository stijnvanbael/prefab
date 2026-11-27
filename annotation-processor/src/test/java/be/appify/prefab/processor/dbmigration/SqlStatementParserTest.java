package be.appify.prefab.processor.dbmigration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlStatementParserTest {

    @Test
    void inlineForeignKeyIsRecordedInColumnState() {
        var tables = new HashMap<String, Table>();
        tables.put(
                "customer_order_history", new Table(
                        "customer_order_history",
                        List.of(new Column("id", new DataType.Varchar(255), false, null, null)),
                        List.of("id")
                ));
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
        tables.put(
                "course", new Table(
                        "course",
                        List.of(new Column("id", new DataType.Varchar(255), false, null, null)),
                        List.of("id")
                ));
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

        var error = assertThrows(
                MigrationSqlException.class,
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

    @Test
    void duplicateTableAndColumnDefinitionsFail() {
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        CREATE TABLE chapter (id BIGINT);
                        """, "Cannot create table chapter: it already exists");
        assertInvalid(
                "CREATE TABLE chapter (id VARCHAR(255), id BIGINT);",
                "column id is defined more than once");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        ALTER TABLE chapter ADD COLUMN id BIGINT;
                        """, "Cannot add column id to table chapter: it already exists");
    }

    @Test
    void duplicateIndexAndNamedConstraintDefinitionsFail() {
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        CREATE INDEX chapter_id_ix ON chapter (id);
                        CREATE INDEX chapter_id_ix ON chapter (id);
                        """, "index chapter_id_ix: it already exists");
        assertInvalid(
                """
                        CREATE TABLE course (id VARCHAR(255) PRIMARY KEY);
                        CREATE TABLE chapter (id VARCHAR(255), course_id VARCHAR(255));
                        ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                            FOREIGN KEY (course_id) REFERENCES course (id);
                        ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                            FOREIGN KEY (course_id) REFERENCES course (id);
                        """, "constraint chapter_course_fk");
    }

    @Test
    void namedPrimaryAndUniqueConstraintsReserveTheirBackingIndexNames() {
        assertInvalid(
                """
                        CREATE TABLE chapter (
                            id VARCHAR(255),
                            CONSTRAINT chapter_id_unique UNIQUE (id),
                            CONSTRAINT chapter_id_unique UNIQUE (id)
                        );
                        """, "constraint chapter_id_unique");
        assertInvalid(
                """
                        CREATE TABLE chapter (
                            id VARCHAR(255),
                            CONSTRAINT chapter_id_unique UNIQUE (id)
                        );
                        CREATE INDEX chapter_id_unique ON chapter (id);
                        """, "index chapter_id_unique: it already exists");
        assertInvalid(
                """
                        CREATE TABLE chapter (
                            id VARCHAR(255),
                            CONSTRAINT chapter_id_unique UNIQUE (id)
                        );
                        CREATE TABLE course (
                            id VARCHAR(255),
                            CONSTRAINT chapter_id_unique UNIQUE (id)
                        );
                        """, "backing index already exists");
    }

    @Test
    void tableAndColumnRenamesCannotReplaceExistingNames() {
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        CREATE TABLE course (id VARCHAR(255));
                        ALTER TABLE chapter RENAME TO course;
                        """, "a table with that name already exists");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255), title VARCHAR(255));
                        ALTER TABLE chapter RENAME COLUMN id TO title;
                        """, "a column with that name already exists");
    }

    @Test
    void unguardedDropsOfMissingObjectsFail() {
        assertInvalid("DROP TABLE missing;", "Cannot drop missing table missing");
        assertInvalid("DROP INDEX missing_ix;", "Cannot drop missing index missing_ix");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        ALTER TABLE chapter DROP CONSTRAINT missing_constraint;
                        """, "Cannot drop missing constraint missing_constraint");
    }

    @Test
    void guardedNoOpsPreserveExistingSchema() {
        var tables = new HashMap<String, Table>();
        new SqlStatementParser().parse(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        CREATE TABLE IF NOT EXISTS chapter (replacement BIGINT);
                        ALTER TABLE chapter ADD COLUMN IF NOT EXISTS id BIGINT;
                        CREATE INDEX chapter_id_ix ON chapter (id);
                        CREATE INDEX IF NOT EXISTS chapter_id_ix ON chapter (replacement);
                        DROP TABLE IF EXISTS "absent";
                        DROP INDEX IF EXISTS absent_ix;
                        ALTER TABLE chapter DROP CONSTRAINT IF EXISTS absent_constraint;
                        """, tables);

        assertTrue(tables.get("chapter").getColumn("replacement").isEmpty());
        assertEquals("VARCHAR (255)", tables.get("chapter").getColumn("id").orElseThrow().type().toSql());
        assertTrue(tables.get("chapter").getIndex("chapter_id_ix").isPresent());
    }

    @Test
    void indexAndForeignKeyDeclarationsRequireExistingColumnsAndTables() {
        assertInvalid(
                "CREATE INDEX missing_table_ix ON missing_table (id);",
                "on missing table missing_table");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        CREATE INDEX chapter_missing_ix ON chapter (missing);
                        """, "Index chapter_missing_ix on table chapter references missing column missing");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        CREATE INDEX chapter_id_ix ON chapter (id);
                        CREATE INDEX IF NOT EXISTS chapter_id_ix ON missing_table (id);
                        """, "on missing table missing_table");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        ALTER TABLE chapter ADD CONSTRAINT chapter_missing_fk
                            FOREIGN KEY (missing) REFERENCES course (id);
                        """, "foreign key on table chapter references missing column missing");
        assertInvalid(
                """
                        CREATE TABLE course (other VARCHAR(255));
                        CREATE TABLE chapter (id VARCHAR(255), course_id VARCHAR(255));
                        ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                            FOREIGN KEY (course_id) REFERENCES course (missing);
                        """, "referenced side of foreign key on table course references missing column missing");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255), course_id VARCHAR(255));
                        ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                            FOREIGN KEY (course_id) REFERENCES course (id);
                        """, "Foreign key on table chapter references missing table course");
        assertInvalid(
                """
                        CREATE TABLE chapter (id VARCHAR(255));
                        ALTER TABLE chapter ADD COLUMN course_id VARCHAR(255) REFERENCES missing_course (id);
                        """, "Foreign key on table chapter references missing table missing_course");
    }

    @Test
    void referencesEstablishedEarlierInTheMigrationAreAccepted() {
        var tables = new HashMap<String, Table>();
        new SqlStatementParser().parse(
                """
                        CREATE TABLE course (id VARCHAR(255) PRIMARY KEY);
                        CREATE TABLE chapter (id VARCHAR(255), course_id VARCHAR(255));
                        CREATE INDEX chapter_course_ix ON chapter (course_id);
                        ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                            FOREIGN KEY (course_id) REFERENCES course (id);
                        ALTER TABLE chapter ADD COLUMN "other_course_id" VARCHAR(255) REFERENCES course (id);
                        """, tables);

        assertTrue(tables.get("chapter").getIndex("chapter_course_ix").isPresent());
        assertTrue(tables.get("chapter").hasConstraint("chapter_course_fk"));
        assertNotNull(tables.get("chapter").getColumn("other_course_id").orElseThrow().foreignKey());
    }

    @Test
    void guardedDropsAndAltersOfMissingTablesAreNoOps() {
        var tables = new HashMap<String, Table>();
        new SqlStatementParser().parse(
                """
                        DROP TABLE IF EXISTS missing;
                        DROP INDEX IF EXISTS missing_ix;
                        ALTER TABLE IF EXISTS missing ADD COLUMN id VARCHAR(255);
                        """, tables);
        assertTrue(tables.isEmpty());
    }

    private static void assertInvalid(String sql, String expectedMessage) {
        var error = assertThrows(
                MigrationSqlException.class,
                () -> new SqlStatementParser().parse(sql, new HashMap<>()));
        var statements = sql.strip().split(";");
        var offendingStatement = statements[statements.length - 1].isBlank()
                ? statements[statements.length - 2].trim()
                : statements[statements.length - 1].trim();
        assertTrue(error.getMessage().contains("statement '" + offendingStatement + "'"), error::getMessage);
        assertTrue(error.getMessage().contains(expectedMessage), error::getMessage);
    }

    private Table tableWithForeignKey() {
        return new Table(
                "chapter",
                List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null),
                        new Column(
                                "course", new DataType.Varchar(255), false,
                                new ForeignKeyReference("course", "id"), null)
                ),
                List.of("id")
        );
    }
}
