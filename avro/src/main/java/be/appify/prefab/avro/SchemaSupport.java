package be.appify.prefab.avro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

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
     * Logical type for representing a reference to another aggregate root,
     * stored as a string containing the ID of the referenced aggregate root.
     */
    public static final LogicalType REFERENCE = new LogicalType("reference");

    /**
     * Creates an Avro schema for a logical type by wrapping a primitive schema with logical type metadata.
     *
     * @param type        the primitive Avro type (e.g., {@code Schema.Type.STRING}, {@code Schema.Type.LONG})
     * @param logicalType the logical type annotation to attach (e.g., {@code "uuid"}, {@code "decimal"})
     * @return a new schema representing the logical type
     * @see LogicalType#addToSchema(Schema)
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
        if (schema.getType() == Schema.Type.UNION) {
            var types = new HashSet<>(schema.getTypes());
            types.add(Schema.create(Schema.Type.NULL));
            return Schema.createUnion(new ArrayList<>(types));
        }
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

    /**
     * Adds a {@code "sample"} property to the given Avro field and returns it.
     *
     * @param field
     *         the Avro field to annotate
     * @param sample
     *         the sample value to attach
     * @return the same field instance with the {@code "sample"} property set
     */
    public static Schema.Field withSample(Schema.Field field, String sample) {
        field.addProp("sample", sample);
        return field;
    }

    /**
     * Sets the {@code doc} property on the given Avro field and returns it.
     *
     * @param field
     *         the Avro field to document
     * @param doc
     *         the human-readable description to attach
     * @return the same field instance with the {@code doc} property set
     */
    public static Schema.Field withDoc(Schema.Field field, String doc) {
        field.addProp("doc", doc);
        return field;
    }

    /**
     * Finds the first schema with the given simple name within the given schema tree.
     *
     * @param schema    the root schema to search
     * @param simpleName the simple name of the schema to find (e.g. {@code "User"})
     * @return the matching schema
     * @throws IllegalArgumentException if no matching schema is found
     */
    public static Schema namedTypeOf(Schema schema, String simpleName) {
        return namedTypeOf(schema, simpleName, new HashSet<>())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No named type '%s' found in schema: %s".formatted(simpleName, schema)));
    }

    private static Optional<Schema> namedTypeOf(Schema schema, String simpleName, Set<String> visited) {
        if (schema == null) {
            return Optional.empty();
        }
        return switch (schema.getType()) {
            case RECORD -> findNamedInRecord(schema, simpleName, visited);
            case ENUM -> findNamedInEnum(schema, simpleName);
            case ARRAY -> findNamedInArray(schema, simpleName, visited);
            case UNION -> findNamedInUnion(schema, simpleName, visited);
            default -> Optional.empty();
        };
    }

    private static Optional<Schema> findNamedInRecord(Schema schema, String simpleName, Set<String> visited) {
        var fullName = schema.getFullName();
        if (fullName != null && !visited.add(fullName)) {
            return Optional.empty();
        }
        if (schema.getName().equals(simpleName)) {
            return Optional.of(schema);
        }
        for (var field : schema.getFields()) {
            var match = namedTypeOf(field.schema(), simpleName, visited);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private static Optional<Schema> findNamedInEnum(Schema schema, String simpleName) {
        return schema.getName().equals(simpleName) ? Optional.of(schema) : Optional.empty();
    }

    private static Optional<Schema> findNamedInArray(Schema schema, String simpleName, Set<String> visited) {
        return namedTypeOf(schema.getElementType(), simpleName, visited);
    }

    private static Optional<Schema> findNamedInUnion(Schema schema, String simpleName, Set<String> visited) {
        for (var member : schema.getTypes()) {
            var match = namedTypeOf(member, simpleName, visited);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
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

    /**
     * Extracts a plain record schema from a field schema, unwrapping it from a union if necessary.
     *
     * @param schema the field schema, either a record or a union containing one
     * @return the record schema
     * @throws IllegalArgumentException if no record type exists in a union
     */
    public static Schema recordSchemaOf(Schema schema) {
        return unwrapUnion(schema, Schema.Type.RECORD);
    }

    /**
     * Determines whether a schema is a record, either directly or wrapped in a union.
     *
     * @param schema the schema to check
     * @return {@code true} if the schema or any union member is a record
     */
    public static boolean isRecordSchema(Schema schema) {
        if (schema.getType() == Schema.Type.RECORD) {
            return true;
        }
        if (schema.getType() != Schema.Type.UNION) {
            return false;
        }
        return schema.getTypes().stream().anyMatch(s -> s.getType() == Schema.Type.RECORD);
    }

    /**
     * Builds a {@link GenericRecord} containing a single field with the given value.
     *
     * @param schema    the record schema (unwrapped from a union if necessary)
     * @param fieldName the name of the field to set
     * @param fieldValue the value to assign to the field
     * @return a new {@code GenericRecord} with the field populated
     */
    public static GenericRecord singleValueRecord(Schema schema, String fieldName, Object fieldValue) {
        var recordSchema = recordSchemaOf(schema);
        var record = new GenericData.Record(recordSchema);
        record.put(fieldName, fieldValue);
        return record;
    }

    /**
     * Ensures the given schema is nullable, adding a null branch if absent.
     *
     * @param schema the schema to make nullable
     * @return a schema whose type union includes {@code null}
     */
    public static Schema createNullableUnion(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            var types = new ArrayList<>(schema.getTypes());
            if (types.stream().noneMatch(t -> t.getType() == Schema.Type.NULL)) {
                types.addFirst(Schema.create(Schema.Type.NULL));
            }
            return Schema.createUnion(types);
        }
        return Schema.createUnion(Schema.create(Schema.Type.NULL), schema);
    }

    /**
     * Resolves a named branch from a union schema.
     *
     * @param unionSchema the union schema to inspect
     * @param branchName  the simple name of the branch to find
     * @return the matching branch schema
     * @throws IllegalArgumentException if no such branch exists
     */
    public static Schema namedBranchOf(Schema unionSchema, String branchName) {
        if (unionSchema.getType() == Schema.Type.UNION) {
            return unionSchema.getTypes().stream()
                    .filter(t -> branchName.equals(t.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No branch named '%s' in union: %s".formatted(branchName, unionSchema)));
        }
        if (branchName.equals(unionSchema.getName())) {
            return unionSchema;
        }
        throw new IllegalArgumentException(
                "Schema is not a union and does not have branch '%s': %s".formatted(branchName, unionSchema));
    }
}
