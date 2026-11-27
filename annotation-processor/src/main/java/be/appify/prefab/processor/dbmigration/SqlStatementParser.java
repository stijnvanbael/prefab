package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SqlStatementParser {

    private static final Pattern DROP_CONSTRAINT_PATTERN = Pattern.compile(
            "^ALTER\\s+TABLE\\s+(IF\\s+EXISTS\\s+)?\"?(\\w+)\"?\\s+DROP\\s+CONSTRAINT\\s+"
                    + "(IF\\s+EXISTS\\s+)?\"?(\\w+)\"?\\s*;?$",
            Pattern.CASE_INSENSITIVE
    );

    // JSqlParser cannot parse a function call as index expression
    private static final Pattern FULL_TEXT_INDEX_PATTERN = Pattern.compile(
            "CREATE\\s+INDEX\\s+\"?(\\w+)\"?\\s+ON\\s+\"?(\\w+)\"?\\s+USING\\s+GIN\\s+\\(((?:to_tsvector|\\(setweight)\\(.*\\))\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern INITIALS_INDEX_PATTERN = Pattern.compile(
            "CREATE\\s+INDEX\\s+\"?(\\w+)\"?\\s+ON\\s+\"?(\\w+)\"?\\s+\\((regexp_replace\\(.*\\s+text_pattern_ops)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    void parse(String content, Map<String, Table> tables) {
        splitStatements(content).forEach(sql -> parseStatement(sql, tables));
    }

    private List<String> splitStatements(String content) {
        return Arrays.stream(content.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isEmpty())
                .toList();
    }

    private void parseStatement(String sql, Map<String, Table> tables) {
        try {
            var dropConstraintMatcher = DROP_CONSTRAINT_PATTERN.matcher(sql);
            if (dropConstraintMatcher.matches()) {
                applyDropConstraint(dropConstraintMatcher, tables);
                return;
            }
            var fullTextIndexMatcher = FULL_TEXT_INDEX_PATTERN.matcher(sql);
            if (fullTextIndexMatcher.matches()) {
                applyExpressionIndex(fullTextIndexMatcher, "GIN", tables);
                return;
            }
            var initialsIndexMatcher = INITIALS_INDEX_PATTERN.matcher(sql);
            if (initialsIndexMatcher.matches()) {
                applyExpressionIndex(initialsIndexMatcher, null, tables);
                return;
            }
            if (sql.regionMatches(true, 0, "CREATE EXTENSION", 0, "CREATE EXTENSION".length())) {
                return;
            }
            applyParsedStatement(sql, tables);
        } catch (MigrationSqlException e) {
            throw e;
        } catch (Exception e) {
            throw new MigrationSqlException(
                    "statement '%s' failed: %s".formatted(sql, e.getMessage()), e);
        }
    }

    private void applyParsedStatement(String sql, Map<String, Table> tables) throws Exception {
        var statement = new CCJSqlParser(sql).Statement();
        if (statement instanceof Insert || statement instanceof Update || statement instanceof Delete) {
            // Migration DML does not change the schema facts used to generate a migration.
            return;
        }
        if (statement instanceof CreateTable createTable) {
            applyCreateTable(createTable, tables);
        } else if (statement instanceof Alter alter) {
            applyAlter(alter, tables);
        } else if (statement instanceof CreateIndex createIndex) {
            applyCreateIndex(createIndex, tables);
        } else if (statement instanceof Drop drop && "INDEX".equalsIgnoreCase(drop.getType())) {
            applyDropIndex(drop, tables);
        } else if (statement instanceof Drop drop && "TABLE".equalsIgnoreCase(drop.getType())) {
            applyDropTable(drop, tables);
        } else {
            throw new IllegalStateException("Prefab cannot model schema statement of type "
                    + statement.getClass().getSimpleName());
        }
    }

    private static void applyCreateTable(CreateTable createTable, Map<String, Table> tables) {
        var tableName = normalizeIdentifier(createTable.getTable().getName());
        if (tables.containsKey(tableName)) {
            if (createTable.isIfNotExists()) {
                return;
            }
            throw new IllegalStateException("Cannot create table " + tableName + ": it already exists");
        }
        var table = Table.fromCreateTable(createTable);
        validateTable(table, tables);
        tables.put(table.name(), table);
    }

    private static void validateTable(Table table, Map<String, Table> tables) {
        var columns = new HashSet<String>();
        for (var column : table.columns()) {
            if (!columns.add(column.name())) {
                throw new IllegalStateException("Cannot create table " + table.name()
                        + ": column " + column.name() + " is defined more than once");
            }
        }
        validateColumnsExist(table.name(), "primary key", table.primaryKey(), table);
        validateDuplicateConstraints(table);

        for (var constraint : table.namedConstraints()) {
            if (isIndexBackedConstraint(constraint) && indexExists(tables, constraint.name())) {
                throw new IllegalStateException("Cannot create constraint " + constraint.name()
                        + " on table " + table.name() + ": its backing index already exists");
            }
            if (constraint.type() != null && constraint.type().contains("UNIQUE")) {
                validateColumnsExist(table.name(), "constraint " + constraint.name(), constraint.columns(), table);
            }
        }
        for (var foreignKey : table.foreignKeys()) {
            validateForeignKey(
                    table, foreignKey.columnNames(),
                    foreignKey.reference(), tables);
        }
        for (var column : table.columns()) {
            if (column.foreignKey() != null) {
                validateForeignKey(table, List.of(column.name()), column.foreignKey(), tables);
            }
        }
    }

    private static void validateDuplicateConstraints(Table table) {
        var names = new HashSet<String>();
        for (var constraint : table.namedConstraints()) {
            if (!names.add(constraint.name())) {
                throw new IllegalStateException("Cannot define constraint " + constraint.name()
                        + " on table " + table.name() + ": that constraint name is already in use");
            }
        }
    }

    private static void validateForeignKey(
            Table localTable,
            List<String> localColumns,
            ForeignKeyReference reference,
            Map<String, Table> tables
    ) {
        validateColumnsExist(localTable.name(), "foreign key", localColumns, localTable);
        validateForeignKeyReference(localTable, localColumns, reference, tables);
    }

    private static void validateForeignKeyReference(
            Table localTable,
            List<String> localColumns,
            ForeignKeyReference reference,
            Map<String, Table> tables
    ) {
        var referencedTable = tables.get(reference.referencedTable());
        if (referencedTable == null && localTable.name().equals(reference.referencedTable())) {
            referencedTable = localTable;
        }
        if (referencedTable == null) {
            throw new IllegalStateException("Foreign key on table " + localTable.name()
                    + " references missing table " + reference.referencedTable());
        }
        validateColumnsExist(
                referencedTable.name(), "referenced side of foreign key",
                reference.referencedColumns(), referencedTable);
        if (localColumns.size() != reference.referencedColumns().size()) {
            throw new IllegalStateException("Foreign key on table " + localTable.name()
                    + " has " + localColumns.size() + " local columns but "
                    + reference.referencedColumns().size() + " referenced columns");
        }
    }

    private static void validateColumnsExist(
            String tableName,
            String declaration,
            List<String> columnNames,
            Table table
    ) {
        for (var columnName : columnNames) {
            var normalizedName = normalizeIdentifier(columnName);
            if (table.getColumn(normalizedName).isEmpty()) {
                throw new IllegalStateException(declaration + " on table " + tableName
                        + " references missing column " + normalizedName);
            }
        }
    }

    private void applyExpressionIndex(Matcher matcher, String using, Map<String, Table> tables) {
        var index = new Index(matcher.group(1), List.of(matcher.group(3)), false, using);
        createIndex(matcher.group(2), index, false, tables);
    }

    private void applyDropConstraint(Matcher matcher, Map<String, Table> tables) {
        var tableName = matcher.group(2);
        var table = tables.get(tableName);
        if (table == null) {
            if (matcher.group(1) != null) {
                return;
            }
            throw new IllegalStateException("Cannot drop constraint from missing table " + tableName);
        }
        var constraintName = matcher.group(4);
        if (!table.hasConstraint(constraintName)) {
            if (matcher.group(3) != null) {
                return;
            }
            throw new IllegalStateException("Cannot drop missing constraint " + constraintName
                    + " from table " + tableName);
        }
        tables.put(tableName, table.withDroppedConstraint(constraintName));
    }

    private void applyAlter(Alter alter, Map<String, Table> tables) {
        var tableName = normalizeIdentifier(alter.getTable().getName());
        var table = tables.get(tableName);
        if (table == null) {
            if (alter.isUseTableIfExists()) {
                return;
            }
            throw new IllegalStateException("Cannot alter missing table " + tableName);
        }
        validateAlter(alter, table, tables);
        var updated = table.apply(alter);
        if (!updated.name().equals(table.name())) {
            if (tables.containsKey(updated.name())) {
                throw new IllegalStateException("Cannot rename table " + table.name() + " to "
                        + updated.name() + ": a table with that name already exists");
            }
            tables.remove(table.name());
        }
        tables.put(updated.name(), updated);
    }

    private static void validateAlter(Alter alter, Table table, Map<String, Table> tables) {
        for (AlterExpression expression : alter.getAlterExpressions()) {
            if (expression.getOperation() == AlterOperation.ADD && expression.hasColumn()) {
                var column = Column.fromAddColumn(expression);
                var existingColumn = table.getColumn(column.name()).isPresent();
                if (existingColumn && !expression.isUseIfNotExists()) {
                    throw new IllegalStateException("Cannot add column " + column.name() + " to table "
                            + table.name() + ": it already exists");
                }
                if (!existingColumn && column.foreignKey() != null) {
                    validateForeignKeyReference(table, List.of(column.name()), column.foreignKey(), tables);
                }
            } else if (expression.getOperation() == AlterOperation.RENAME
                    && expression.getColumnOldName() != null) {
                var oldName = normalizeIdentifier(expression.getColumnOldName());
                var newName = normalizeIdentifier(expression.getColumnName());
                if (table.getColumn(oldName).isEmpty()) {
                    throw new IllegalStateException("Cannot rename missing column " + oldName
                            + " on table " + table.name());
                }
                if (table.getColumn(newName).isPresent()) {
                    throw new IllegalStateException("Cannot rename column " + oldName + " to " + newName
                            + " on table " + table.name() + ": a column with that name already exists");
                }
            } else if (expression.getOperation() == AlterOperation.RENAME_TABLE) {
                var newName = normalizeIdentifier(expression.getNewTableName());
                if (!newName.equals(table.name()) && tables.containsKey(newName)) {
                    throw new IllegalStateException("Cannot rename table " + table.name() + " to " + newName
                            + ": a table with that name already exists");
                }
            } else if (expression.getOperation() == AlterOperation.DROP
                    && expression.getConstraintName() != null
                    && !table.hasConstraint(expression.getConstraintName())
                    && !expression.isUsingIfExists()) {
                throw new IllegalStateException("Cannot drop missing constraint "
                        + normalizeIdentifier(expression.getConstraintName())
                        + " from table " + table.name());
            } else if (expression.getOperation() == AlterOperation.ADD
                    && expression.getIndex() instanceof ForeignKeyIndex foreignKeyIndex) {
                var foreignKey = ForeignKeyConstraint.fromIndex(table.name(), foreignKeyIndex);
                if (table.hasConstraint(foreignKey.name())) {
                    throw new IllegalStateException("Cannot add constraint " + foreignKey.name()
                            + " to table " + table.name() + ": that constraint name is already in use");
                }
                validateForeignKey(table, foreignKey.columnNames(), foreignKey.reference(), tables);
            }
        }
    }

    private void applyCreateIndex(CreateIndex createIndex, Map<String, Table> tables) {
        var index = Index.fromCreateIndex(createIndex);
        createIndex(
                normalizeIdentifier(createIndex.getTable().getName()),
                index, createIndex.isUsingIfNotExists(), tables);
    }

    private void createIndex(String tableName, Index index, boolean ifNotExists, Map<String, Table> tables) {
        var table = tables.get(tableName);
        if (table == null) {
            throw new IllegalStateException("Cannot create index " + index.name()
                    + " on missing table " + tableName);
        }
        if (indexExists(tables, index.name())) {
            if (ifNotExists) {
                return;
            }
            throw new IllegalStateException("Cannot create index " + index.name() + ": it already exists");
        }
        validateIndexColumns(index, table);
        tables.put(tableName, table.withAddedIndex(index));
    }

    private static boolean indexExists(Map<String, Table> tables, String indexName) {
        return tables.values().stream().anyMatch(table -> table.getIndex(indexName).isPresent()
                || table.namedConstraints().stream().anyMatch(constraint -> indexName.equals(constraint.name())
                && isIndexBackedConstraint(constraint)));
    }

    private static boolean isIndexBackedConstraint(Table.NamedConstraint constraint) {
        return constraint.type() != null
                && (constraint.type().contains("PRIMARY KEY") || constraint.type().contains("UNIQUE"));
    }

    private static void validateIndexColumns(Index index, Table table) {
        for (var column : index.columns()) {
            if (!column.contains("(") && table.getColumn(normalizeIdentifier(column)).isEmpty()) {
                throw new IllegalStateException("Index " + index.name() + " on table " + table.name()
                        + " references missing column " + normalizeIdentifier(column));
            }
        }
    }

    private void applyDropIndex(Drop drop, Map<String, Table> tables) {
        var indexName = normalizeIdentifier(drop.getName().getName());
        var table = tables.values().stream()
                .filter(candidate -> candidate.getIndex(indexName).isPresent())
                .findFirst();
        if (table.isEmpty()) {
            var constraint = tables.values().stream()
                    .flatMap(candidate -> candidate.namedConstraints().stream())
                    .filter(candidate -> candidate.name().equals(indexName) && isIndexBackedConstraint(candidate))
                    .findFirst();
            if (constraint.isPresent()) {
                throw new IllegalStateException("Cannot drop index " + indexName
                        + ": it backs constraint " + constraint.get().name());
            }
            if (drop.isIfExists()) {
                return;
            }
            throw new IllegalStateException("Cannot drop missing index " + indexName);
        }
        tables.put(table.get().name(), table.get().withRemovedIndex(indexName));
    }

    private void applyDropTable(Drop drop, Map<String, Table> tables) {
        var tableName = normalizeIdentifier(drop.getName().getName());
        if (!tables.containsKey(tableName)) {
            if (drop.isIfExists()) {
                return;
            }
            throw new IllegalStateException("Cannot drop missing table " + tableName);
        }
        tables.remove(tableName);
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier.replace("\"", "");
    }
}
