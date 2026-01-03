package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

record Table(
        String name,
        List<Column> columns,
        List<String> primaryKey
) {
    private static final List<String> PRIMARY_KEY = List.of("PRIMARY", "KEY");
    private static final String FOREIGN_KEY = "FOREIGN KEY";

    static Table fromCreateTable(CreateTable createTable) {
        return new Table(
                createTable.getTable().getName().replace("\"", ""),
                createTable.getColumnDefinitions().stream()
                        .map(Column::fromColumnDefinition)
                        .toList(),
                primaryKey(createTable)
        );
    }

    private static List<String> primaryKey(CreateTable createTable) {
        if (createTable.getIndexes() == null || createTable.getIndexes().isEmpty()) {
            return createTable.getColumnDefinitions().stream()
                    .filter(column -> column.getColumnSpecs() != null && Collections.indexOfSubList(
                            column.getColumnSpecs(), PRIMARY_KEY) != -1)
                    .map(column -> column.getColumnName().replace("\"", ""))
                    .toList();
        }
        return createTable.getIndexes().stream()
                .filter(idx -> idx.getType().equals("PRIMARY KEY"))
                .flatMap(idx -> idx.getColumnsNames().stream()
                        .map(name -> name.replace("\"", "")))
                .toList();
    }

    public Optional<Column> getColumn(String name) {
        return columns.stream()
                .filter(c -> Objects.equals(c.name(), name))
                .findFirst();
    }

    public Table apply(Alter alter) {
        return new Table(
                name,
                mapColumns(alter),
                primaryKey
        );
    }

    private List<Column> mapColumns(Alter alter) {
        var columns = this.columns.stream()
                .collect(Collectors.toMap(Column::name, c -> c, (v1, v2) -> {
                    throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                }, LinkedHashMap::new));
        alter.getAlterExpressions().forEach(expr -> {
            switch (expr.getOperation()) {
                case AlterOperation.ADD -> applyAddExpression(expr, columns);
                case DROP -> applyDropExpression(expr, columns);
                case ALTER -> applyAlterExpression(expr, columns);
                default -> throw new IllegalStateException("Unsupported ALTER TABLE expression: " + expr);
            }
        });
        return List.copyOf(columns.values());
    }

    private static void applyAlterExpression(AlterExpression expr, Map<String, Column> columns) {
        if (!expr.hasColumn()) {
            throw new IllegalArgumentException("Unsupported ALTER TABLE expression: " + expr);
        }
        var column = expr.getColDataTypeList().getFirst();
        var original = columns.get(column.getColumnName());
        if (original == null) {
            throw new IllegalStateException("Cannot alter non-existing column: " + column.getColumnName());
        }
        var modified = original.withDataType(
                DataType.parse(expr.getColDataTypeList().getFirst().getColDataType().getDataType()));
        columns.put(modified.name(), modified);
    }

    private static void applyDropExpression(AlterExpression expr, Map<String, Column> columns) {
        if (!expr.hasColumn()) {
            throw new IllegalArgumentException("Unsupported ALTER TABLE expression: " + expr);
        }
        columns.remove(expr.getColumnName());
    }

    private static void applyAddExpression(AlterExpression expr, Map<String, Column> columns) {
        if (expr.hasColumn()) {
            var column = Column.fromAddColumn(expr);
            columns.put(column.name(), column);
        } else if (expr.getIndex() != null) {
            if (!expr.getIndex().getType().equals(FOREIGN_KEY)) {
                throw new IllegalArgumentException(
                        "Unsupported ADD CONSTRAINT expression, expected FOREIGN KEY: " + expr);
            }
            var column = columns.get(expr.getIndex().getColumnsNames().getFirst());
            if (column == null) {
                throw new IllegalStateException("Cannot add foreign key to non-existing column: " + expr.getIndex()
                        .getColumnsNames().getFirst());
            }
            var modified = column.withForeignKey(ForeignKey.fromIndex((ForeignKeyIndex) expr.getIndex()));
            columns.put(modified.name(), modified);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported ALTER TABLE expression, expected ADD COLUMN or ADD CONSTRAINT: " + expr);
        }
    }
}
