package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbDefaultValue;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.VariableManifest;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.util.Collections;
import java.util.List;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

public record Column(
        String name,
        DataType type,
        boolean nullable,
        ForeignKey foreignKey,
        String defaultValue
) {
    private static final List<String> NOT_NULL = List.of("NOT", "NULL");

    public static Column fromColumnDefinition(ColumnDefinition column) {
        return createColumn(
                column.getColumnName(),
                column.getColDataType(),
                column.getColumnSpecs() != null ? column.getColumnSpecs() : Collections.emptyList()
        );
    }

    public static Column fromAddColumn(AlterExpression alterExpression) {
        var column = alterExpression.getColDataTypeList().getFirst();
        return createColumn(
                column.getColumnName(),
                column.getColDataType(),
                column.getColumnSpecs() != null ? column.getColumnSpecs() : Collections.emptyList()
        );
    }

    private static Column createColumn(String name, ColDataType dataType, List<String> columnSpecs) {
        return new Column(
                name,
                DataType.parse(dataType.toString()),
                Collections.indexOfSubList(columnSpecs, NOT_NULL) == -1,
                columnSpecs.contains("REFERENCES") ? ForeignKey.fromColumnSpecs(columnSpecs) : null,
                columnSpecs.contains("DEFAULT") ? columnSpecs.get(columnSpecs.indexOf("DEFAULT") + 1) : null
        );
    }

    public static Column fromField(String prefix, VariableManifest property, boolean parentNullable) {
        var name = prefix != null ? prefix + "_" + property.name() : property.name();
        return new Column(
                toSnakeCase(name),
                DataType.typeOf(property.toBoxed().type(), property.annotations()),
                parentNullable || property.nullable(),
                property.type().is(Reference.class) ? new ForeignKey(
                        toSnakeCase(property.type().parameters().getFirst().simpleName()),
                        "id"
                ) : null,
                property.getAnnotation(DbDefaultValue.class)
                        .map(defaultValue -> defaultValue.value().value())
                        .orElse(null)
        );
    }

    public Column withDataType(DataType dataType) {
        return new Column(
                name,
                dataType,
                nullable,
                foreignKey,
                defaultValue
        );
    }

    public Column withForeignKey(ForeignKey foreignKey) {
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
        return name + " " + type
                + (nullable ? "" : " NOT NULL")
                + (defaultValue != null ? " DEFAULT " + defaultValue : "")
                + (foreignKey != null ? " " + foreignKey : "");
    }
}
