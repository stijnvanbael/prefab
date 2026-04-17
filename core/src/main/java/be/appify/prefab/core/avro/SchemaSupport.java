package be.appify.prefab.core.avro;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 * Utility class for creating Avro schemas with logical types.
 */
public class SchemaSupport {
    private SchemaSupport() {
    }

    /**
     * Logical type for representing an instant in time, stored as a long representing milliseconds since the epoch.
     */
    public static final LogicalType DURATION_MILLIS = new LogicalType("duration-millis");
    /**
     * Logical type for representing a reference to another aggregate root, stored as a string containing the ID of the referenced aggregate
     * root.
     */
    public static final LogicalType REFERENCE = new LogicalType("reference");

    /**
     * Creates an Avro schema for a logical type by adding the logical type information to a primitive schema.
     *
     * @param type
     *         the primitive type to use for the schema (e.g., Schema.Type.LONG for DURATION_MILLIS)
     * @param logicalType
     *         the logical type to add to the schema
     * @return a Schema instance representing the logical type
     */
    public static Schema createLogicalSchema(Schema.Type type, LogicalType logicalType) {
        var schema = Schema.create(type);
        logicalType.addToSchema(schema);
        return schema;
    }

    /**
     * Creates a nullable Avro schema by wrapping the provided schema in a union with null.
     *
     * @param schema
     *         the schema to make nullable
     * @return a Schema instance representing the nullable schema
     */
    public static Schema createNullableSchema(Schema schema) {
        return Schema.createUnion(Schema.create(Schema.Type.NULL), schema);
    }

    /**
     * Resolves the array schema from a potentially nullable union schema.
     * When a list field is declared as nullable, Avro wraps the array schema in a union with null.
     * {@link org.apache.avro.generic.GenericData.Array} requires a plain array schema, not a union.
     *
     * @param schema
     *         the field schema, either a plain array schema or a nullable union containing one
     * @return the array schema
     */
    public static Schema arraySchemaOf(Schema schema) {
        return unwrapUnion(schema, Schema.Type.ARRAY);
    }

    /**
     * Resolves the enum schema from a potentially nullable union schema.
     * When an enum field is declared as nullable, Avro wraps the enum schema in a union with null.
     * {@link org.apache.avro.generic.GenericData.EnumSymbol} requires a plain enum schema, not a union.
     *
     * @param schema
     *         the field schema, either a plain enum schema or a nullable union containing one
     * @return the enum schema
     */
    public static Schema enumSchemaOf(Schema schema) {
        return unwrapUnion(schema, Schema.Type.ENUM);
    }

    private static Schema unwrapUnion(Schema schema, Schema.Type targetType) {
        if (schema.getType() != Schema.Type.UNION) {
            return schema;
        }
        return schema.getTypes().stream()
                .filter(s -> s.getType() == targetType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No %s schema found in union: %s".formatted(targetType, schema)));
    }
}
