package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.drop.Drop;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SqlStatementParser {

    private static final Pattern DROP_CONSTRAINT_PATTERN = Pattern.compile(
            "ALTER\\s+TABLE\\s+\"?(\\w+)\"?\\s+DROP\\s+CONSTRAINT\\s+\"?(\\w+)\"?",
            Pattern.CASE_INSENSITIVE
    );

    // JSqlParser cannot parse a function call as index expression
    private static final Pattern FULL_TEXT_INDEX_PATTERN = Pattern.compile(
            "CREATE\\s+INDEX\\s+\"?(\\w+)\"?\\s+ON\\s+\"?(\\w+)\"?\\s+USING\\s+GIN\\s+\\(((?:to_tsvector|setweight)\\(.*\\))\\)",
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
        var dropConstraintMatcher = DROP_CONSTRAINT_PATTERN.matcher(sql);
        if (dropConstraintMatcher.find()) {
            applyDropConstraint(dropConstraintMatcher.group(1), dropConstraintMatcher.group(2), tables);
            return;
        }
        var fullTextIndexMatcher = FULL_TEXT_INDEX_PATTERN.matcher(sql);
        if (fullTextIndexMatcher.matches()) {
            applyFullTextIndex(fullTextIndexMatcher, tables);
            return;
        }
        parseWithJsqlparser(sql, tables);
    }

    private void applyFullTextIndex(Matcher matcher, Map<String, Table> tables) {
        var table = tables.get(matcher.group(2));
        if (table != null) {
            var index = new Index(matcher.group(1), List.of(matcher.group(3)), false, "GIN");
            tables.put(table.name(), table.withAddedIndex(index));
        }
    }

    private void applyDropConstraint(String tableName, String constraintName, Map<String, Table> tables) {
        var table = tables.get(tableName);
        if (table == null) {
            return;
        }
        var updated = table.withDroppedConstraint(constraintName);
        tables.put(tableName, updated);
    }

    private void parseWithJsqlparser(String sql, Map<String, Table> tables) {
        try {
            var statement = new CCJSqlParser(sql).Statement();
            if (statement instanceof CreateTable createTable) {
                var table = Table.fromCreateTable(createTable);
                tables.put(table.name(), table);
            } else if (statement instanceof Alter alter) {
                applyAlter(alter, tables);
            } else if (statement instanceof CreateIndex createIndex) {
                applyCreateIndex(createIndex, tables);
            } else if (statement instanceof Drop drop && "INDEX".equalsIgnoreCase(drop.getType())) {
                applyDropIndex(drop, tables);
            } else if (statement instanceof Drop drop && "TABLE".equalsIgnoreCase(drop.getType())) {
                applyDropTable(drop, tables);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SQL statement: " + sql, e);
        }
    }

    private void applyAlter(Alter alter, Map<String, Table> tables) {
        var tableName = alter.getTable().getName().replace("\"", "");
        var table = tables.get(tableName);
        if (table == null) {
            throw new IllegalStateException("Found ALTER TABLE on table not previously created: " + alter.getTable().getName());
        }
        var updated = table.apply(alter);
        if (!updated.name().equals(table.name())) {
            tables.remove(table.name());
        }
        tables.put(updated.name(), updated);
    }

    private void applyCreateIndex(CreateIndex createIndex, Map<String, Table> tables) {
        var tableName = createIndex.getTable().getName().replace("\"", "");
        var table = tables.get(tableName);
        if (table != null) {
            tables.put(tableName, table.withAddedIndex(Index.fromCreateIndex(createIndex)));
        }
    }

    private void applyDropIndex(Drop drop, Map<String, Table> tables) {
        var indexName = drop.getName().getName().replace("\"", "");
        tables.values().stream()
                .filter(t -> t.getIndex(indexName).isPresent())
                .findFirst()
                .ifPresent(t -> tables.put(t.name(), t.withRemovedIndex(indexName)));
    }

    private void applyDropTable(Drop drop, Map<String, Table> tables) {
        var tableName = drop.getName().getName().replace("\"", "");
        tables.remove(tableName);
    }
}

