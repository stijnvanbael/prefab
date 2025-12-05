package be.appify.prefab.processor.dbmigration;

import java.util.List;
import java.util.Objects;

public interface TableModification {
    static List<TableModification> from(String table, Column original, Column desired) {
        if (original != null && desired != null) {
            var modifications = new java.util.ArrayList<TableModification>();
            if (!original.type().equals(desired.type())) {
                modifications.add(new AlterColumn(original.name(),
                        new ColumnModification.ChangeType(original.name(), desired.type())));
            }
            if (original.nullable() != desired.nullable()) {
                modifications.add(
                        new AlterColumn(original.name(),
                                new ColumnModification.ChangeNotNull(table + "." + original.name(), desired.nullable(),
                                        desired.defaultValue())));
            }
            if (!Objects.equals(original.foreignKey(), desired.foreignKey())) {
                if (original.foreignKey() != null) {
                    modifications.add(new DropForeignKey(table, original.name()));
                }
                if (desired.foreignKey() != null) {
                    modifications.add(new AddForeignKey(table, desired.name(), desired.foreignKey()));
                }
            }
            return modifications;
        } else if (original == null && desired != null) {
            return List.of(new AddColumn(desired));
        } else if (original != null) {
            return List.of(new DropColumn(original.name()));
        } else {
            throw new IllegalStateException("Both original and desired columns are null");
        }
    }

    String toSql();

    record AddColumn(Column column) implements TableModification {
        @Override
        public String toSql() {
            if (!column.nullable() && column.defaultValue() == null) {
                throw new IllegalStateException(
                        "Cannot add a NOT NULL column [%s] without a default value. Annotate the corresponding field with @DbDefaultValue to specify it."
                                .formatted(column.name()));
            }
            return "ADD COLUMN " + column;
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record DropColumn(String columnName) implements TableModification {
        @Override
        public String toSql() {
            return "DROP COLUMN " + columnName;
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record AlterColumn(String columnName, ColumnModification modification) implements TableModification {
        @Override
        public String toSql() {
            return "ALTER COLUMN " + columnName + " " + modification.toSql();
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record AddForeignKey(String tableName, String columnName, ForeignKey foreignKey) implements TableModification {
        @Override
        public String toSql() {
            return "ADD CONSTRAINT fk_" + tableName + "_" + columnName + " FOREIGN KEY (\"" + columnName + "\") " + foreignKey.toString();
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record DropForeignKey(String tableName, String columnName) implements TableModification {
        @Override
        public String toSql() {
            return "DROP CONSTRAINT fk_" + tableName + "_" + columnName;
        }

        @Override
        public String toString() {
            return toSql();
        }
    }
}
