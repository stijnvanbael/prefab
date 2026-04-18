package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbDefaultValue;
import be.appify.prefab.core.annotations.DbRename;
import be.appify.prefab.processor.VariableManifest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

record Column(
        String name,
        DataType type,
        boolean nullable,
        ForeignKey foreignKey,
        String defaultValue,
        String oldName
) {
    Column(String name, DataType type, boolean nullable, ForeignKey foreignKey, String defaultValue) {
        this(name, type, nullable, foreignKey, defaultValue, null);
    }

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
        var parsedType = DataType.parse(dataType.toString());
        var rawDefault = columnSpecs.contains("DEFAULT")
                ? unquoteSqlDefault(columnSpecs.get(columnSpecs.indexOf("DEFAULT") + 1))
                : null;
        return new Column(
                name.replace("\"", ""),
                parsedType,
                Collections.indexOfSubList(columnSpecs, NOT_NULL) == -1,
                columnSpecs.contains("REFERENCES") ? ForeignKey.fromColumnSpecs(columnSpecs) : null,
                rawDefault,
                null
        );
    }

    private static String unquoteSqlDefault(String token) {
        if (token != null && token.startsWith("'") && token.endsWith("'") && token.length() >= 2) {
            return token.substring(1, token.length() - 1).replace("''", "'");
        }
        return token;
    }

    static Column fromField(String prefix, VariableManifest property, boolean parentNullable) {
        return fromField(prefix, property, parentNullable,
                DataType.typeOf(property.type().asBoxed(), property.annotations()));
    }

    static Column fromField(String prefix, VariableManifest property, boolean parentNullable, DataType dataType) {
        var rawName = prefix != null ? prefix + "_" + property.name() : property.name();
        var oldName = resolveOldName(prefix, property);
        return new Column(
                toSnakeCase(rawName),
                dataType,
                parentNullable || property.nullable(),
                null,
                property.getAnnotation(DbDefaultValue.class)
                        .map(defaultValue -> defaultValue.value().value())
                        .orElse(null),
                oldName
        );
    }

    private static Optional<String> resolveOldNameOptional(String prefix, VariableManifest property) {
        return property.getAnnotation(DbRename.class)
                .map(ann -> {
                    var rawOldName = prefix != null ? prefix + "_" + ann.value().value() : ann.value().value();
                    return toSnakeCase(rawOldName);
                });
    }

    private static String resolveOldName(String prefix, VariableManifest property) {
        return resolveOldNameOptional(prefix, property).orElse(null);
    }

    Column withDataType(DataType dataType) {
        return new Column(
                name,
                dataType,
                nullable,
                foreignKey,
                defaultValue,
                oldName
        );
    }

    Column withForeignKey(ForeignKey foreignKey) {
        return new Column(
                name,
                type,
                nullable,
                foreignKey,
                defaultValue,
                oldName
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Column other)) return false;
        return Objects.equals(name, other.name)
                && Objects.equals(type, other.type)
                && nullable == other.nullable
                && Objects.equals(foreignKey, other.foreignKey)
                && Objects.equals(defaultValue, other.defaultValue);
        // oldName is intentionally excluded: it is a migration hint (@DbRename), not part of the schema
        // definition. Two Column instances with the same schema but different oldName values must compare
        // as equal so that change detection works correctly.
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, nullable, foreignKey, defaultValue);
    }

    String formattedDefaultValue() {
        if (defaultValue == null) {
            return null;
        }
        if (type.requiresQuoting()) {
            return "'" + defaultValue.replace("'", "''") + "'";
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "\"" + name + "\" " + type
                + (nullable ? "" : " NOT NULL")
                + (defaultValue != null ? " DEFAULT " + formattedDefaultValue() : "")
                + (foreignKey != null ? " " + foreignKey : "");
    }
}
