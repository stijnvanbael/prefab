package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.processor.ListUtil;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Objects;

public interface DatabaseChange {
    String toSql();

    record CreateTable(Table table) implements DatabaseChange {
        @Override
        public String toSql() {
            var columnsSql = table.columns().stream()
                    .map(Column::toString)
                    .collect(joining(",\n"));
            var primaryKeySql = table.primaryKey().isEmpty() ? "" :
                    ", PRIMARY KEY(" + String.join(", ", table.primaryKey()) + ")\n";
            return "CREATE TABLE " + table.name() + " (\n" +
                   columnsSql + "\n" +
                   primaryKeySql +
                   ");\n";
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

    record AlterTable(String tableName, List<TableModification> modifications) implements DatabaseChange {
        @Override
        public String toSql() {
            var modificationsSql = modifications.stream()
                    .map(TableModification::toSql)
                    .collect(joining(",\n"));
            return "ALTER TABLE " + tableName + "\n" +
                   modificationsSql +
                   ";\n";
        }

        @Override
        public String toString() {
            return toSql();
        }

        static AlterTable from(Table existing, Table desired) {
            var toModify = existing.columns().stream()
                    .filter(c -> !Objects.equals(c, desired.getColumn(c.name()).orElse(null)))
                    .flatMap(c -> {
                        var desiredColumn = desired.getColumn(c.name()).orElse(null);
                        return TableModification.from(existing.name(), c, desiredColumn).stream();
                    })
                    .toList();
            var toAdd = desired.columns().stream()
                    .filter(c -> existing.getColumn(c.name()).isEmpty())
                    .flatMap(c -> TableModification.from(existing.name(), null, c).stream())
                    .toList();
            return new AlterTable(existing.name(), ListUtil.concat(toModify, toAdd));
        }
    }
}
