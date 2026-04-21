package be.appify.prefab.processor.dbmigration;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SqlStatementParserTest {

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

