package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.util.IdentifierShortener;
import net.sf.jsqlparser.statement.create.index.CreateIndex;

import java.util.List;

import static be.appify.prefab.core.util.IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH;
import static be.appify.prefab.core.util.IdentifierShortener.shorten;
import static java.util.stream.Collectors.joining;

record Index(
        String name,
        List<String> columns,
        boolean unique,
        String using
) {
    Index(String name, List<String> columns, boolean unique) {
        this(name, columns, unique, null);
    }

    static Index of(String tableName, String columnName, boolean unique) {
        return new Index(
                IdentifierShortener.indexName(tableName, columnName, unique),
                List.of(columnName),
                unique,
                null
        );
    }

    static Index gin(String tableName, String columnName) {
        var name = shorten(tableName + "_" + columnName + "_gin", POSTGRES_MAX_IDENTIFIER_LENGTH);
        return new Index(name, List.of(columnName), false, "GIN");
    }

    static Index jsonbPath(String tableName, String jsonbColumn, String fieldName, boolean unique) {
        var name = IdentifierShortener.indexName(tableName, jsonbColumn + "_" + fieldName, unique);
        var expression = "(\"%s\"->>'%s')".formatted(jsonbColumn, fieldName);
        return new Index(name, List.of(expression), unique, null);
    }

    static Index fromCreateIndex(CreateIndex createIndex) {
        var indexType = createIndex.getIndex().getType();
        boolean unique = "UNIQUE".equalsIgnoreCase(indexType);
        String using = (!unique && indexType != null) ? indexType : null;
        return new Index(
                createIndex.getIndex().getName().replace("\"", ""),
                createIndex.getIndex().getColumnsNames().stream()
                        .map(Index::normalizeColumnName)
                        .toList(),
                unique,
                using
        );
    }

    private static String normalizeColumnName(String columnName) {
        if (isExpression(columnName)) {
            return columnName;
        }
        return columnName.replace("\"", "");
    }

    String toCreateSql(String tableName) {
        var usingClause = using != null ? "USING " + using + " " : "";
        var columnsSql = columns.stream()
                .map(c -> isExpression(c) ? c : "\"" + c + "\"")
                .collect(joining(", "));
        return "CREATE " + (unique ? "UNIQUE " : "") + "INDEX \"" + name + "\" ON \""
                + tableName + "\" " + usingClause + "(" + columnsSql + ");\n";
    }

    String toDropSql() {
        return "DROP INDEX \"" + name + "\";\n";
    }

    private static boolean isExpression(String column) {
        return column.startsWith("(");
    }
}
