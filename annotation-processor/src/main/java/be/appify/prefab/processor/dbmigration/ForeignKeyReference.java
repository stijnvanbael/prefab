package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.List;
import java.util.stream.Collectors;

record ForeignKeyReference(
        String referencedTable,
        List<String> referencedColumns
) {
    ForeignKeyReference(String referencedTable, String referencedColumn) {
        this(referencedTable, List.of(referencedColumn));
    }

    static ForeignKeyReference fromColumnSpecs(List<String> columnSpecs) {
        var referencedTable = columnSpecs.get(columnSpecs.indexOf("REFERENCES") + 1);
        var referencedColumn = columnSpecs.get(columnSpecs.indexOf("REFERENCES") + 2);
        return new ForeignKeyReference(
                referencedTable.replace("\"", ""),
                referencedColumn.substring(1, referencedColumn.length() - 1).replace("\"", "")
        );
    }

    static ForeignKeyReference fromIndex(ForeignKeyIndex index) {
        return new ForeignKeyReference(
                index.getTable().getName(),
                index.getReferencedColumnNames()
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
