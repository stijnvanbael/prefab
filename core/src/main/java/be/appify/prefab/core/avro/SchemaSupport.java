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
}
