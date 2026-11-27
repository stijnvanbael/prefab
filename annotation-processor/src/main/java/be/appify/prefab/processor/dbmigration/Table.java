package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.util.IdentifierShortener;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

record Table(
        String name,
        List<Column> columns,
        List<String> primaryKey,
        String oldName,
        List<Index> indexes,
        List<ForeignKeyConstraint> foreignKeys,
        List<NamedConstraint> namedConstraints
) {
    private static final List<String> PRIMARY_KEY = List.of("PRIMARY", "KEY");
    private static final String FOREIGN_KEY = "FOREIGN KEY";

    record NamedConstraint(String name, String type, List<String> columns) {
    }

    Table(String name, List<Column> columns, List<String> primaryKey) {
        this(name, columns, primaryKey, null, List.of(), List.of(), List.of());
    }

    Table(String name, List<Column> columns, List<String> primaryKey, String oldName) {
        this(name, columns, primaryKey, oldName, List.of(), List.of(), List.of());
    }

    Table(String name, List<Column> columns, List<String> primaryKey, List<Index> indexes) {
        this(name, columns, primaryKey, null, indexes, List.of(), List.of());
    }

    Table(String name, List<Column> columns, List<String> primaryKey, String oldName, List<Index> indexes) {
        this(name, columns, primaryKey, oldName, indexes, List.of(), List.of());
    }

    Table(
            String name, List<Column> columns, List<String> primaryKey, String oldName, List<Index> indexes,
            List<ForeignKeyConstraint> foreignKeys
    ) {
        this(name, columns, primaryKey, oldName, indexes, foreignKeys, namedForeignKeys(foreignKeys));
    }

    static Table fromCreateTable(CreateTable createTable) {
        return new Table(
                createTable.getTable().getName().replace("\"", ""),
                createTable.getColumnDefinitions().stream()
                        .map(Column::fromColumnDefinition)
                        .toList(),
                primaryKey(createTable),
                null,
                List.of(),
                foreignKeys(createTable),
                namedConstraints(createTable)
        );
    }

    private static List<ForeignKeyConstraint> foreignKeys(CreateTable createTable) {
        if (createTable.getIndexes() == null || createTable.getIndexes().isEmpty()) {
            return List.of();
        }
        return createTable.getIndexes().stream()
                .filter(index -> FOREIGN_KEY.equals(index.getType()))
                .map(index -> ForeignKeyConstraint.fromIndex(
                        createTable.getTable().getName().replace("\"", ""),
                        (ForeignKeyIndex) index))
                .toList();
    }

    private static List<NamedConstraint> namedConstraints(CreateTable createTable) {
        if (createTable.getIndexes() == null || createTable.getIndexes().isEmpty()) {
            return namedForeignKeys(foreignKeys(createTable));
        }
        var constraints = new ArrayList<>(createTable.getIndexes().stream()
                .filter(index -> index.getName() != null)
                .map(index -> new NamedConstraint(
                        normalizeIdentifier(index.getName()),
                        index.getType(),
                        index.getColumnsNames() == null
                                ? List.of()
                                : index.getColumnsNames().stream().map(Table::normalizeIdentifier).toList()))
                .toList());
        var namedNames = constraints.stream().map(NamedConstraint::name).collect(Collectors.toSet());
        namedForeignKeys(foreignKeys(createTable)).stream()
                .filter(constraint -> namedNames.add(constraint.name()))
                .forEach(constraints::add);
        return List.copyOf(constraints);
    }

    private static List<NamedConstraint> namedForeignKeys(List<ForeignKeyConstraint> foreignKeys) {
        return foreignKeys.stream()
                .map(foreignKey -> new NamedConstraint(
                        foreignKey.name(), FOREIGN_KEY, foreignKey.columnNames()))
                .toList();
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier.replace("\"", "");
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
        var newColumns = mapColumns(alter);
        var newName = extractNewTableName(alter);
        var newForeignKeys = new ArrayList<>(foreignKeys);
        var newNamedConstraints = new ArrayList<>(namedConstraints);
        var newPrimaryKey = new ArrayList<>(primaryKey);
        alter.getAlterExpressions().forEach(expr -> {
            if (expr.getOperation() == AlterOperation.ADD && expr.getIndex() instanceof ForeignKeyIndex foreignKeyIndex) {
                var foreignKey = ForeignKeyConstraint.fromIndex(name, foreignKeyIndex);
                newForeignKeys.add(foreignKey);
                if (foreignKeyIndex.getName() != null) {
                    newNamedConstraints.add(new NamedConstraint(
                            foreignKey.name(), FOREIGN_KEY, foreignKey.columnNames()));
                }
            } else if (expr.getOperation() == AlterOperation.DROP && expr.getConstraintName() != null) {
                var constraintName = normalizeIdentifier(expr.getConstraintName());
                var dropped = newNamedConstraints.stream()
                        .filter(constraint -> constraint.name().equals(constraintName))
                        .findFirst();
                dropped.ifPresent(constraint -> {
                    newNamedConstraints.remove(constraint);
                    if (constraint.type() != null && constraint.type().contains("PRIMARY KEY")) {
                        newPrimaryKey.clear();
                    }
                });
                newForeignKeys.removeIf(foreignKey -> foreignKey.name().equals(constraintName));
            }
        });
        return new Table(
                newName != null ? newName : name,
                newColumns,
                List.copyOf(newPrimaryKey),
                null,
                indexes,
                List.copyOf(newForeignKeys),
                List.copyOf(newNamedConstraints)
        );
    }

    private String extractNewTableName(Alter alter) {
        return alter.getAlterExpressions().stream()
                .filter(expr -> expr.getOperation() == AlterOperation.RENAME_TABLE)
                .map(expr -> expr.getNewTableName().replace("\"", ""))
                .findFirst()
                .orElse(null);
    }

    public Table withAddedIndex(Index index) {
        var newIndexes = new ArrayList<>(indexes);
        newIndexes.add(index);
        return new Table(name, columns, primaryKey, null, List.copyOf(newIndexes), foreignKeys, namedConstraints);
    }

    public Table withRemovedIndex(String indexName) {
        var newIndexes = indexes.stream()
                .filter(idx -> !idx.name().equals(indexName))
                .toList();
        return new Table(name, columns, primaryKey, null, newIndexes, foreignKeys, namedConstraints);
    }

    public Table withDroppedConstraint(String constraintName) {
        var normalizedName = normalizeIdentifier(constraintName);
        var dropped = namedConstraints.stream()
                .filter(constraint -> constraint.name().equals(normalizedName))
                .findFirst();
        var newColumns = columns.stream()
                .map(c -> c.foreignKey() != null && (
                        IdentifierShortener.foreignKeyConstraintName(name, c.name()).equals(normalizedName)
                                || dropped.map(constraint -> constraint.columns().contains(c.name())).orElse(false))
                        ? c.withForeignKey(null)
                        : c)
                .toList();
        var newForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> !foreignKey.name().equals(normalizedName))
                .toList();
        var newNamedConstraints = namedConstraints.stream()
                .filter(constraint -> !constraint.name().equals(normalizedName))
                .toList();
        var newPrimaryKey = dropped
                .filter(constraint -> constraint.type() != null && constraint.type().contains("PRIMARY KEY"))
                .isPresent()
                ? List.<String>of()
                : primaryKey;
        return new Table(name, newColumns, newPrimaryKey, null, indexes, newForeignKeys, newNamedConstraints);
    }

    public boolean hasConstraint(String constraintName) {
        var normalizedName = normalizeIdentifier(constraintName);
        return namedConstraints.stream().anyMatch(constraint -> constraint.name().equals(normalizedName))
                || foreignKeys.stream().anyMatch(constraint -> constraint.name().equals(normalizedName))
                || columns.stream().anyMatch(column -> column.foreignKey() != null
                && IdentifierShortener.foreignKeyConstraintName(name, column.name()).equals(normalizedName));
    }

    public Optional<Index> getIndex(String name) {
        return indexes.stream()
                .filter(idx -> idx.name().equals(name))
                .findFirst();
    }

    private List<Column> mapColumns(Alter alter) {
        var columns = this.columns.stream()
                .collect(Collectors.toMap(
                        Column::name, c -> c, (v1, v2) -> {
                            throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                        }, LinkedHashMap::new));
        alter.getAlterExpressions().forEach(expr -> {
            switch (expr.getOperation()) {
                case AlterOperation.ADD -> applyAddExpression(expr, columns);
                case DROP -> applyDropExpression(expr, columns);
                case ALTER -> applyAlterExpression(expr, columns);
                case RENAME -> applyRenameColumnExpression(expr, columns);
                case RENAME_TABLE -> { /* table rename is handled in apply() */ }
                default -> throw new IllegalStateException("Unsupported ALTER TABLE expression: " + expr);
            }
        });
        return List.copyOf(columns.values());
    }

    private static void applyRenameColumnExpression(AlterExpression expr, Map<String, Column> columns) {
        var oldName = expr.getColumnOldName().replace("\"", "");
        var newName = expr.getColumnName().replace("\"", "");
        var original = columns.get(oldName);
        if (original == null) {
            throw new IllegalStateException("Cannot rename non-existing column: " + oldName);
        }
        if (columns.containsKey(newName)) {
            throw new IllegalStateException("Cannot rename column " + oldName + " to " + newName
                    + ": a column with that name already exists");
        }
        columns.remove(oldName);
        var renamed = new Column(
                newName, original.type(), original.nullable(), original.foreignKey(),
                original.defaultValue(), null);
        columns.put(newName, renamed);
    }

    private static void applyAlterExpression(AlterExpression expr, Map<String, Column> columns) {
        if (!expr.hasColumn()) {
            throw new IllegalArgumentException("ALTER TABLE expression currently not supported: " + expr);
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

    private void applyDropExpression(AlterExpression expr, Map<String, Column> columns) {
        if (expr.hasColumn()) {
            var columnName = expr.getColumnName().replace("\"", "");
            if (!columns.containsKey(columnName) && !expr.isUsingIfExists()) {
                throw new IllegalStateException("Cannot drop non-existing column: " + columnName);
            }
            columns.remove(columnName);
        } else if (isNotBlank(expr.getConstraintName())) {
            var constraintName = normalizeIdentifier(expr.getConstraintName());
            var constraint = namedConstraints.stream()
                    .filter(candidate -> candidate.name().equals(constraintName))
                    .findFirst();
            columns.values().stream()
                    .filter(c -> c.foreignKey() != null && (
                            IdentifierShortener.foreignKeyConstraintName(name, c.name()).equals(constraintName)
                                    || constraint.map(candidate -> candidate.columns().contains(c.name())).orElse(false)))
                    .findFirst()
                    .ifPresent(column -> {
                        var modified = column.withForeignKey(null);
                        columns.put(modified.name(), modified);
                    });
        } else {
            throw new IllegalArgumentException("ALTER TABLE expression currently not supported: " + expr);
        }
    }

    private static void applyAddExpression(AlterExpression expr, Map<String, Column> columns) {
        if (expr.hasColumn()) {
            var column = Column.fromAddColumn(expr);
            if (columns.containsKey(column.name())) {
                if (expr.isUseIfNotExists()) {
                    return;
                }
                throw new IllegalStateException("Cannot add column " + column.name() + ": it already exists");
            }
            columns.put(column.name(), column);
        } else if (expr.getIndex() != null) {
            if (!expr.getIndex().getType().equals(FOREIGN_KEY)) {
                throw new IllegalArgumentException(
                        "Unsupported ADD CONSTRAINT expression, expected FOREIGN KEY: " + expr);
            }
            if (expr.getIndex().getColumnsNames().size() == 1) {
                var column = columns.get(expr.getIndex().getColumnsNames().getFirst());
                if (column == null) {
                    throw new IllegalStateException("Cannot add foreign key to non-existing column: " + expr.getIndex()
                            .getColumnsNames().getFirst());
                }
                var modified = column.withForeignKey(ForeignKeyReference.fromIndex((ForeignKeyIndex) expr.getIndex()));
                columns.put(modified.name(), modified);
            } else {
                throw new IllegalArgumentException(
                        "Composite foreign keys in ALTER TABLE are currently not supported: " + expr);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported ALTER TABLE expression, expected ADD COLUMN or ADD CONSTRAINT: " + expr);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table other)) return false;
        return Objects.equals(name, other.name)
                && Objects.equals(columns, other.columns)
                && Objects.equals(primaryKey, other.primaryKey);
        // oldName is intentionally excluded: it is a migration hint (@DbRename), not part of the current
        // table definition. Two Table instances with the same schema but different oldName values must
        // compare as equal so that change detection works correctly.
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, primaryKey);
    }
}
