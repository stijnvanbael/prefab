package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

record ForeignKeyReference(
        String referencedTable,
        List<String> referencedColumns
) {
    ForeignKeyReference(String referencedTable, String referencedColumn) {
        this(referencedTable, List.of(referencedColumn));
    }

    static Optional<ForeignKeyReference> fromColumnSpecs(List<String> columnSpecs) {
        for (int i = 0; i < columnSpecs.size(); i++) {
            var spec = columnSpecs.get(i).trim();
            if (spec.equalsIgnoreCase("REFERENCES")) {
                if (i + 2 >= columnSpecs.size()) {
                    throw new IllegalArgumentException("Incomplete column REFERENCES clause");
                }
                return Optional.of(new ForeignKeyReference(
                        unquote(columnSpecs.get(i + 1)),
                        unquoteReferenceColumn(columnSpecs.get(i + 2))
                ));
            }
            if (startsWithReferences(spec)) {
                return Optional.of(parseColumnReference(spec));
            }
        }
        return Optional.empty();
    }

    private static ForeignKeyReference parseColumnReference(String spec) {
        var clause = spec.substring("REFERENCES".length()).trim();
        var openParenthesis = clause.indexOf('(');
        var closeParenthesis = clause.lastIndexOf(')');
        if (openParenthesis <= 0 || closeParenthesis <= openParenthesis) {
            throw new IllegalArgumentException("Invalid column REFERENCES clause: " + spec);
        }
        var referencedTable = clause.substring(0, openParenthesis).trim();
        var referencedColumns = Arrays.stream(clause.substring(openParenthesis + 1, closeParenthesis).split(","))
                .map(ForeignKeyReference::unquote)
                .toList();
        if (referencedTable.isEmpty() || referencedColumns.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("Invalid column REFERENCES clause: " + spec);
        }
        return new ForeignKeyReference(unquote(referencedTable), referencedColumns);
    }

    private static boolean startsWithReferences(String spec) {
        return spec.regionMatches(true, 0, "REFERENCES", 0, "REFERENCES".length())
                && spec.length() > "REFERENCES".length()
                && Character.isWhitespace(spec.charAt("REFERENCES".length()));
    }

    private static String unquote(String identifier) {
        var trimmed = identifier.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

    private static String unquoteReferenceColumn(String column) {
        var trimmed = column.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return unquote(trimmed.substring(1, trimmed.length() - 1));
        }
        return unquote(trimmed);
    }

    static ForeignKeyReference fromIndex(ForeignKeyIndex index) {
        return new ForeignKeyReference(
                index.getTable().getName().replace("\"", ""),
                index.getReferencedColumnNames().stream()
                        .map(column -> column.replace("\"", ""))
                        .toList()
        );
    }

    @Override
    public String toString() {
        var columns = referencedColumns.stream()
                .map(column -> "\"" + column + "\"")
                .collect(Collectors.joining(", "));
        return "REFERENCES \"" + referencedTable + "\"(" + columns + ")";
    }
}
