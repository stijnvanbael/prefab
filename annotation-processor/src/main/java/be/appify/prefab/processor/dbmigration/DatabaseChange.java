package be.appify.prefab.processor.dbmigration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

interface DatabaseChange {
    String toSql();

    record CreateTable(Table table) implements DatabaseChange {
        @Override
        public String toSql() {
            var definitions = new ArrayList<>(table.columns().stream().map(Column::toString).toList());

            if (!table.primaryKey().isEmpty()) {
                var primaryKeyColumns = table.primaryKey().stream()
                        .map(column -> "\"" + column + "\"")
                        .collect(joining(", "));
                definitions.add("PRIMARY KEY(" + primaryKeyColumns + ")");
            }

            definitions.addAll(table.foreignKeys().stream().map(ForeignKeyConstraint::toSql).toList());

            return "CREATE TABLE \"" + table.name() + "\" (\n  " +
                    String.join(",\n  ", definitions) +
                    "\n);\n";
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record DropTable(String tableName) implements DatabaseChange {
        @Override
        public String toSql() {
            return "DROP TABLE " + tableName + ";\n";
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record RenameTable(String oldName, String newName) implements DatabaseChange {
        @Override
        public String toSql() {
            return "ALTER TABLE \"" + oldName + "\" RENAME TO \"" + newName + "\";\n";
        }

        @Override
        public String toString() {
            return toSql();
        }
    }


    record CreateIndex(String tableName, Index index) implements DatabaseChange {
        @Override
        public String toSql() {
            return index.toCreateSql(tableName);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record DropIndex(Index index) implements DatabaseChange {
        @Override
        public String toSql() {
            return index.toDropSql();
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record EnableExtension(String extensionName) implements DatabaseChange {
        @Override
        public String toSql() {
            return "CREATE EXTENSION IF NOT EXISTS \"%s\";\n".formatted(extensionName);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record AlterTable(String tableName, List<TableModification> modifications) implements DatabaseChange {
        @Override
        public String toSql() {
            var sql = new StringBuilder();
            var batch = new ArrayList<TableModification>();
            for (var modification : modifications) {
                if (modification instanceof TableModification.RenameColumn) {
                    appendStatement(sql, batch);
                    batch.add(modification);
                    appendStatement(sql, batch);
                } else {
                    batch.add(modification);
                }
            }
            appendStatement(sql, batch);
            return sql.toString();
        }

        // PostgreSQL does not allow RENAME COLUMN to be combined with other actions in one ALTER TABLE
        private void appendStatement(StringBuilder sql, List<TableModification> batch) {
            if (batch.isEmpty()) {
                return;
            }
            sql.append("ALTER TABLE ").append(tableName).append("\n")
                    .append(batch.stream().map(TableModification::toSql).collect(joining(",\n")))
                    .append(";\n");
            batch.clear();
        }

        @Override
        public String toString() {
            return toSql();
        }

        static AlterTable from(Table existing, Table desired) {
            // Build rename map: old column name -> desired column (for columns with @DbRename)
            // Two columns with the same oldName would be a model error (can't rename two columns from the same source)
            var renames = desired.columns().stream()
                    .filter(c -> c.oldName() != null && existing.getColumn(c.oldName()).isPresent())
                    .collect(Collectors.toMap(Column::oldName, c -> c, (a, b) -> {
                        throw new IllegalStateException(
                                "Two columns share the same @DbRename old name '%s': '%s' and '%s'"
                                        .formatted(a.oldName(), a.name(), b.name()));
                    }));

            var renamedOldNames = renames.keySet();
            var renamedNewNames = renames.values().stream()
                    .map(Column::name)
                    .collect(Collectors.toSet());

            var modifications = new ArrayList<TableModification>();

            // Drop columns first so that a rename may reuse the name of a removed column
            for (var existingColumn : existing.columns()) {
                var replacedByRename = renamedNewNames.contains(existingColumn.name())
                        && !renamedOldNames.contains(existingColumn.name());
                var removed = desired.getColumn(existingColumn.name()).isEmpty()
                        && !renamedOldNames.contains(existingColumn.name());
                if (replacedByRename || removed) {
                    modifications.add(new TableModification.DropColumn(existingColumn.name()));
                }
            }

            // Handle renames (and any type/nullability changes on the renamed column)
            for (var entry : renames.entrySet()) {
                var oldName = entry.getKey();
                var desiredColumn = entry.getValue();
                var existingColumn = existing.getColumn(oldName).get();

                modifications.add(new TableModification.RenameColumn(oldName, desiredColumn.name()));

                // Check if type or nullability also changed (compare against same-named column)
                var existingAsRenamed = new Column(desiredColumn.name(), existingColumn.type(),
                        existingColumn.nullable(), existingColumn.foreignKey(), existingColumn.defaultValue(), null);
                modifications.addAll(TableModification.from(desired.name(), existingAsRenamed, desiredColumn));
            }

            // Handle existing columns that are not being renamed
            for (var existingColumn : existing.columns()) {
                if (renamedOldNames.contains(existingColumn.name())
                        || renamedNewNames.contains(existingColumn.name())) {
                    continue;
                }
                var desiredColumn = desired.getColumn(existingColumn.name()).orElse(null);
                if (desiredColumn != null && !Objects.equals(existingColumn, desiredColumn)) {
                    modifications.addAll(TableModification.from(desired.name(), existingColumn, desiredColumn));
                }
            }

            // Handle new desired columns that are not rename targets
            for (var desiredColumn : desired.columns()) {
                if (renamedNewNames.contains(desiredColumn.name())) {
                    continue;
                }
                if (existing.getColumn(desiredColumn.name()).isEmpty()) {
                    modifications.addAll(TableModification.from(desired.name(), null, desiredColumn));
                }
            }

            return new AlterTable(desired.name(), modifications);
        }
    }
}
