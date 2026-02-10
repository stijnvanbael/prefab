package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbDefaultValue;
import be.appify.prefab.processor.VariableManifest;
import java.util.Collections;
import java.util.List;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

record Column(
        String name,
        DataType type,
        boolean nullable,
        ForeignKey foreignKey,
        String defaultValue
) {
    private static final List<String> NOT_NULL = List.of("NOT", "NULL");

    static Column fromColumnDefinition(ColumnDefinition column) {
        return createColumn(
                column.getColumnName(),
                column.getColDataType(),
                column.getColumnSpecs() != null ? column.getColumnSpecs() : Collections.emptyList()
        );
    }

    static Column fromAddColumn(AlterExpression alterExpression) {
        var column = alterExpression.getColDataTypeList().getFirst();
        return createColumn(
                column.getColumnName(),
                column.getColDataType(),
                column.getColumnSpecs() != null ? column.getColumnSpecs() : Collections.emptyList()
        );
    }

    private static Column createColumn(String name, ColDataType dataType, List<String> columnSpecs) {
        return new Column(
                name.replace("\"", ""),
                DataType.parse(dataType.toString()),
                Collections.indexOfSubList(columnSpecs, NOT_NULL) == -1,
                columnSpecs.contains("REFERENCES") ? ForeignKey.fromColumnSpecs(columnSpecs) : null,
                columnSpecs.contains("DEFAULT") ? columnSpecs.get(columnSpecs.indexOf("DEFAULT") + 1) : null
        );
    }

    static Column fromField(String prefix, VariableManifest property, boolean parentNullable) {
        var name = prefix != null ? prefix + "_" + property.name() : property.name();
        return new Column(
                toSnakeCase(name),
                DataType.typeOf(property.type().asBoxed(), property.annotations()),
                parentNullable || property.nullable(),
                null,
                property.getAnnotation(DbDefaultValue.class)
                        .map(defaultValue -> defaultValue.value().value())
                        .orElse(null)
        );
    }

    Column withDataType(DataType dataType) {
        return new Column(
                name,
                dataType,
                nullable,
                foreignKey,
                defaultValue
        );
    }

    Column withForeignKey(ForeignKey foreignKey) {
        return new Column(
                name,
                type,
                nullable,
                foreignKey,
                defaultValue
        );
    }

    @Override
    public String toString() {
        return "\"" + name + "\" " + type
                + (nullable ? "" : " NOT NULL")
                + (defaultValue != null ? " DEFAULT " + defaultValue : "")
                + (foreignKey != null ? " " + foreignKey : "");
    }
}
