package be.appify.prefab.avro;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
        return createNullableUnion(schema);
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
     * Retrieves the value of a named field from a {@link GenericRecord}, returning {@code null}
     * when the field is absent from the record's schema.
     *
     * <p>Avro schema evolution may add new fields after messages have been produced. Calling
     * {@code record.get(fieldName)} on a record written with an older schema that does not contain
     * the field throws an {@link org.apache.avro.AvroRuntimeException}. This method guards against
     * that by checking for field presence first.</p>
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value, or {@code null} if the field does not exist in the record's schema
     */
    public static Object getField(GenericRecord record, String fieldName) {
        return record.getSchema().getField(fieldName) != null ? record.get(fieldName) : null;
    }

    /**
     * Reads a field as a {@link String}, returning {@code null} when the field is absent or its
     * value is null. Avro stores strings as {@code CharSequence} (typically {@code Utf8}), so
     * {@code toString()} is called to obtain the Java {@code String}.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value as a {@code String}, or {@code null}
     */
    public static String getString(GenericRecord record, String fieldName) {
        var value = getField(record, fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * Reads a field as an {@link Integer}, returning {@code null} when the field is absent.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code Integer}, or {@code null}
     */
    public static Integer getInteger(GenericRecord record, String fieldName) {
        return (Integer) getField(record, fieldName);
    }

    /**
     * Reads a field as a {@link Long}, returning {@code null} when the field is absent.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code Long}, or {@code null}
     */
    public static Long getLong(GenericRecord record, String fieldName) {
        return (Long) getField(record, fieldName);
    }

    /**
     * Reads a field as a {@link Double}, returning {@code null} when the field is absent.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code Double}, or {@code null}
     */
    public static Double getDouble(GenericRecord record, String fieldName) {
        return (Double) getField(record, fieldName);
    }

    /**
     * Reads a field as a {@link Float}, returning {@code null} when the field is absent.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code Float}, or {@code null}
     */
    public static Float getFloat(GenericRecord record, String fieldName) {
        return (Float) getField(record, fieldName);
    }

    /**
     * Reads a field as a {@link Boolean}, returning {@code null} when the field is absent.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code Boolean}, or {@code null}
     */
    public static Boolean getBoolean(GenericRecord record, String fieldName) {
        return (Boolean) getField(record, fieldName);
    }

    /**
     * Reads a {@code long} field stored as milliseconds since the epoch and converts it to an
     * {@link Instant}, returning {@code null} when the field is absent or its value is null.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value as an {@code Instant}, or {@code null}
     */
    public static Instant getInstant(GenericRecord record, String fieldName) {
        var millis = getLong(record, fieldName);
        return millis != null ? Instant.ofEpochMilli(millis) : null;
    }

    /**
     * Reads an {@code int} field stored as days since the epoch and converts it to a
     * {@link LocalDate}, returning {@code null} when the field is absent or its value is null.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value as a {@code LocalDate}, or {@code null}
     */
    public static LocalDate getLocalDate(GenericRecord record, String fieldName) {
        var epochDay = getInteger(record, fieldName);
        return epochDay != null ? LocalDate.ofEpochDay(epochDay) : null;
    }

    /**
     * Reads a {@code long} field stored as milliseconds and converts it to a {@link Duration},
     * returning {@code null} when the field is absent or its value is null.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value as a {@code Duration}, or {@code null}
     */
    public static Duration getDuration(GenericRecord record, String fieldName) {
        var millis = getLong(record, fieldName);
        return millis != null ? Duration.ofMillis(millis) : null;
    }

    /**
     * Reads a nested-record field as a {@link GenericRecord}, returning {@code null} when the
     * field is absent or its value is null.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code GenericRecord}, or {@code null}
     */
    public static GenericRecord getRecord(GenericRecord record, String fieldName) {
        return (GenericRecord) getField(record, fieldName);
    }

    /**
     * Reads a nested-record field, passes it to {@code converter} and returns the result,
     * short-circuiting to {@code null} when the field is absent or its value is null. This
     * eliminates the boilerplate null-guard that would otherwise be duplicated around every
     * nested-record converter call.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @param converter the function to apply to the nested record when it is present
     * @param <T>       the converted type
     * @return the converter result, or {@code null} if the field is absent or null
     */
    public static <T> T getRecord(GenericRecord record, String fieldName, Function<GenericRecord, T> converter) {
        var nested = getRecord(record, fieldName);
        return nested != null ? converter.apply(nested) : null;
    }

    /**
     * Reads an enum field by looking up the Avro symbol name in the given enum class, returning
     * {@code null} when the field is absent or its value is null.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @param enumClass the enum class to look up the symbol in
     * @param <E>       the enum type
     * @return the matching enum constant, or {@code null}
     */
    public static <E extends Enum<E>> E getEnum(GenericRecord record, String fieldName, Class<E> enumClass) {
        var value = getField(record, fieldName);
        return value != null ? Enum.valueOf(enumClass, value.toString()) : null;
    }

    /**
     * Reads an array field as a {@link GenericData.Array}, returning {@code null} when the field
     * is absent or its value is null.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @return the field value cast to {@code GenericData.Array}, or {@code null}
     */
    public static GenericData.Array<?> getArray(GenericRecord record, String fieldName) {
        return (GenericData.Array<?>) getField(record, fieldName);
    }

    /**
     * Reads an array field, applies {@code converter} to each element, and returns the resulting
     * list, returning {@code null} when the field is absent or its value is null. This eliminates
     * the boilerplate null-guard and streaming code that would otherwise be duplicated around every
     * array field converter call.
     *
     * @param record    the generic record to read from
     * @param fieldName the name of the field to retrieve
     * @param converter the function to apply to each array element
     * @param <T>       the element type after conversion
     * @return the converted list, or {@code null} if the field is absent or null
     */
    public static <T> List<T> getArray(GenericRecord record, String fieldName, Function<Object, T> converter) {
        var array = getArray(record, fieldName);
        return array != null ? array.stream().map(converter).toList() : null;
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
