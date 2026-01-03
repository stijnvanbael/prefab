package be.appify.prefab.processor.dbmigration;

import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.util.List;

record ForeignKey(
        String referencedTable,
        String referencedColumn
) {
    static ForeignKey fromColumnSpecs(List<String> columnSpecs) {
        var referencedTable = columnSpecs.get(columnSpecs.indexOf("REFERENCES") + 1);
        var referencedColumn = columnSpecs.get(columnSpecs.indexOf("REFERENCES") + 2);
        return new ForeignKey(
                referencedTable.replace("\"", ""),
                referencedColumn.substring(1, referencedColumn.length() - 1).replace("\"", "")
        );
    }

    static ForeignKey fromIndex(ForeignKeyIndex index) {
        return new ForeignKey(
                index.getTable().getName(),
                index.getReferencedColumnNames().getFirst()
        );
    }

    @Override
    public String toString() {
        return "REFERENCES \"" + referencedTable + "\"(\"" + referencedColumn + "\")";
    }
}
