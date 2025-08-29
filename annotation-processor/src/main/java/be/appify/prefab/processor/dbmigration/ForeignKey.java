package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.List;

public record ForeignKey(
        String referencedTable,
        String referencedColumn
) {
    public static ForeignKey fromColumnSpecs(List<String> columnSpecs) {
        var referencedTable = columnSpecs.get(columnSpecs.indexOf("REFERENCES") + 1);
        var referencedColumn = columnSpecs.get(columnSpecs.indexOf("REFERENCES") + 2);
        return new ForeignKey(
                referencedTable,
                referencedColumn.substring(1, referencedColumn.length() - 1)
        );
    }

    public static ForeignKey fromIndex(ForeignKeyIndex index) {
        return new ForeignKey(
                index.getTable().getName(),
                index.getReferencedColumnNames().getFirst()
        );
    }

    @Override
    public String toString() {
        return "REFERENCES " + referencedTable + "(" + referencedColumn + ")";
    }
}
