package be.appify.prefab.processor.dbmigration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

class DbRenameTest {

    @Test
    void renameColumnGeneratesRenameColumnSql() {
        var existing = new Table("user", List.of(
                new Column("first_name", new DataType.Varchar(255), false, null, null, null)
        ), List.of("id"));
        var desired = new Table("user", List.of(
                new Column("given_name", new DataType.Varchar(255), false, null, null, "first_name")
        ), List.of("id"));

        var change = DatabaseChange.AlterTable.from(existing, desired);

        assertThat(change.toSql()).contains("RENAME COLUMN \"first_name\" TO \"given_name\"");
        assertThat(change.toSql()).doesNotContain("DROP COLUMN");
        assertThat(change.toSql()).doesNotContain("ADD COLUMN");
    }

    @Test
    void renameColumnWithTypeChangeGeneratesRenameAndAlterSql() {
        var existing = new Table("user", List.of(
                new Column("age", DataType.Primitive.INTEGER, false, null, null, null)
        ), List.of("id"));
        var desired = new Table("user", List.of(
                new Column("user_age", DataType.Primitive.BIGINT, false, null, null, "age")
        ), List.of("id"));

        var change = DatabaseChange.AlterTable.from(existing, desired);

        assertThat(change.toSql()).contains("RENAME COLUMN \"age\" TO \"user_age\"");
        assertThat(change.toSql()).contains("ALTER COLUMN user_age");
    }

    @Test
    void renameColumnWhenOldNameNotFoundFallsBackToDropAndAdd() {
        var existing = new Table("user", List.of(
                new Column("name", new DataType.Varchar(255), false, null, null, null)
        ), List.of("id"));
        var desired = new Table("user", List.of(
                // nullable column so that ADD COLUMN does not require a default value
                new Column("given_name", new DataType.Varchar(255), true, null, null, "non_existent")
        ), List.of("id"));

        var change = DatabaseChange.AlterTable.from(existing, desired);

        // old name doesn't exist in current -> treated as drop + add
        assertThat(change.toSql()).contains("DROP COLUMN name");
        assertThat(change.toSql()).contains("ADD COLUMN");
        assertThat(change.toSql()).doesNotContain("RENAME COLUMN");
    }

    @Test
    void renameTableGeneratesRenameTableSql() {
        var currentTables = List.of(
                new Table("old_user", List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null, null)
                ), List.of("id"))
        );
        var desiredTables = List.of(
                new Table("user", List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null, null)
                ), List.of("id"), "old_user")
        );

        var writer = new DbMigrationWriter(null);
        var changes = writer.detectChanges(currentTables, desiredTables);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)).isInstanceOf(DatabaseChange.RenameTable.class);
        assertThat(changes.get(0).toSql())
                .isEqualTo("ALTER TABLE \"old_user\" RENAME TO \"user\";\n");
    }

    @Test
    void renameTableWithColumnChangeGeneratesRenameAndAlterSql() {
        var currentTables = List.of(
                new Table("old_user", List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null, null),
                        new Column("name", new DataType.Varchar(255), true, null, null, null)
                ), List.of("id"))
        );
        var desiredTables = List.of(
                new Table("user", List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null, null),
                        new Column("name", new DataType.Varchar(255), false, null, "''", null)
                ), List.of("id"), "old_user")
        );

        var writer = new DbMigrationWriter(null);
        var changes = writer.detectChanges(currentTables, desiredTables);

        assertThat(changes).hasSize(2);
        assertThat(changes.get(0)).isInstanceOf(DatabaseChange.RenameTable.class);
        assertThat(changes.get(1)).isInstanceOf(DatabaseChange.AlterTable.class);
        assertThat(changes.get(0).toSql()).contains("ALTER TABLE \"old_user\" RENAME TO \"user\"");
        assertThat(changes.get(1).toSql()).contains("ALTER TABLE user");
    }

    @Test
    void renameTableOldTableNotDropped() {
        var currentTables = List.of(
                new Table("old_user", List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null, null)
                ), List.of("id"))
        );
        var desiredTables = List.of(
                new Table("user", List.of(
                        new Column("id", new DataType.Varchar(255), false, null, null, null)
                ), List.of("id"), "old_user")
        );

        var writer = new DbMigrationWriter(null);
        var changes = writer.detectChanges(currentTables, desiredTables);

        for (var change : changes) {
            assertThat(change).isNotInstanceOf(DatabaseChange.DropTable.class);
        }
    }

    @Test
    void parseRenameColumnFromSql() throws Exception {
        var table = new Table("user", List.of(
                new Column("first_name", new DataType.Varchar(255), false, null, null, null)
        ), List.of("id"));

        var sql = "ALTER TABLE \"user\" RENAME COLUMN \"first_name\" TO \"given_name\"";
        var parser = new net.sf.jsqlparser.parser.CCJSqlParser(sql);
        var stmt = parser.Statement();
        var alter = (net.sf.jsqlparser.statement.alter.Alter) stmt;
        var updatedTable = table.apply(alter);

        assertThat(updatedTable.name()).isEqualTo("user");
        assertThat(updatedTable.getColumn("given_name").isPresent()).isTrue();
        assertThat(updatedTable.getColumn("first_name").isPresent()).isFalse();
    }

    @Test
    void parseRenameTableFromSql() throws Exception {
        var table = new Table("old_user", List.of(
                new Column("id", new DataType.Varchar(255), false, null, null, null)
        ), List.of("id"));

        var sql = "ALTER TABLE \"old_user\" RENAME TO \"user\"";
        var parser = new net.sf.jsqlparser.parser.CCJSqlParser(sql);
        var stmt = parser.Statement();
        var alter = (net.sf.jsqlparser.statement.alter.Alter) stmt;
        var updatedTable = table.apply(alter);

        assertThat(updatedTable.name()).isEqualTo("user");
        assertThat(updatedTable.getColumn("id").isPresent()).isTrue();
    }

    @Test
    void renameColumnSqlFormat() {
        var modification = new TableModification.RenameColumn("first_name", "given_name");
        assertThat(modification.toSql()).isEqualTo("RENAME COLUMN \"first_name\" TO \"given_name\"");
    }

    @Test
    void renameTableSqlFormat() {
        var change = new DatabaseChange.RenameTable("old_user", "user");
        assertThat(change.toSql()).isEqualTo("ALTER TABLE \"old_user\" RENAME TO \"user\";\n");
    }
}
