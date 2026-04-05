package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.statement.create.index.CreateIndex;

import java.util.List;

import static java.util.stream.Collectors.joining;

record Index(
        String name,
        List<String> columns,
        boolean unique
) {
    static Index of(String tableName, String columnName, boolean unique) {
        return new Index(
                tableName + "_" + columnName + (unique ? "_uidx" : "_idx"),
                List.of(columnName),
                unique
        );
    }

    static Index fromCreateIndex(CreateIndex createIndex) {
        boolean unique = "UNIQUE".equalsIgnoreCase(createIndex.getIndex().getType());
        return new Index(
                createIndex.getIndex().getName().replace("\"", ""),
                createIndex.getIndex().getColumnsNames().stream()
                        .map(c -> c.replace("\"", ""))
                        .toList(),
                unique
        );
    }

    String toCreateSql(String tableName) {
        return "CREATE " + (unique ? "UNIQUE " : "") + "INDEX \"" + name + "\" ON \"" + tableName + "\" (" +
                columns.stream().map(c -> "\"" + c + "\"").collect(joining(", ")) + ");\n";
    }

    String toDropSql() {
        return "DROP INDEX \"" + name + "\";\n";
    }
}
