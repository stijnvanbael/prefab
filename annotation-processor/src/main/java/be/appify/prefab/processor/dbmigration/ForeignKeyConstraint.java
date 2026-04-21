package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.util.IdentifierShortener;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.List;
import java.util.stream.Collectors;

record ForeignKeyConstraint(
        String name,
        List<String> columnNames,
        ForeignKeyReference reference
) {
    static ForeignKeyConstraint fromIndex(String tableName, ForeignKeyIndex index) {
        String constraintName;
        constraintName = index.getName() != null ? index.getName().replace("\"", "") : IdentifierShortener.foreignKeyConstraintName(tableName, String.join("_", index.getColumnsNames()));
        return new ForeignKeyConstraint(
                constraintName,
                index.getColumnsNames().stream().map(column -> column.replace("\"", "")).toList(),
                ForeignKeyReference.fromIndex(index)
        );
    }

    String toSql() {
        var columns = columnNames.stream()
                .map(column -> "\"" + column + "\"")
                .collect(Collectors.joining(", "));
        return "CONSTRAINT \"" + name + "\" FOREIGN KEY (" + columns + ") " + reference;
    }
}

