package be.appify.prefab.streams.kafka;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.ReflectData;

/**
 * Reflection-based mapper used when no explicit ConversionService converters are available for key types.
 */
final class AvroKeyRecordMapper {
    private static final ReflectData REFLECT_DATA = ReflectData.AllowNull.get();

    private AvroKeyRecordMapper() {
    }

    static GenericRecord toGenericRecord(Object value) {
        var schema = REFLECT_DATA.getSchema(value.getClass());
        var mapped = toAvroValue(value, schema);
        if (mapped instanceof GenericRecord record) {
            return record;
        }
        throw new IllegalArgumentException("Expected GenericRecord root for key type " + value.getClass().getName());
    }

    static <K> K fromGenericRecord(GenericRecord genericRecord, Class<K> keyType) {
        return keyType.cast(fromAvroValue(genericRecord, keyType));
    }

    private static Object toAvroValue(Object value, Schema schema) {
        if (value == null) {
            return null;
        }
        var effectiveSchema = resolveConcreteSchemaForValue(schema, value);
        return switch (effectiveSchema.getType()) {
            case RECORD -> value instanceof Instant instant
                    ? toInstantRecord(instant, effectiveSchema)
                    : toGenericRecord(value, effectiveSchema);
            case ENUM -> new GenericData.EnumSymbol(effectiveSchema, ((Enum<?>) value).name());
            case ARRAY -> toGenericArray((Collection<?>) value, effectiveSchema.getElementType());
            case MAP -> toGenericMap((Map<?, ?>) value, effectiveSchema.getValueType());
            case STRING -> value.toString();
            case LONG -> value instanceof Instant instant ? instant.toEpochMilli() : value;
            case INT, DOUBLE, FLOAT, BOOLEAN, BYTES, FIXED -> value;
            case UNION -> throw new IllegalStateException("Union should be resolved before mapping");
            default -> throw new IllegalArgumentException(
                    "Unsupported Avro schema type for key serialization: " + effectiveSchema.getType());
        };
    }

    private static GenericRecord toGenericRecord(Object value, Schema recordSchema) {
        var record = new GenericData.Record(recordSchema);
        for (var field : recordSchema.getFields()) {
            var fieldValue = extractFieldValue(value, field.name());
            record.put(field.name(), toAvroValue(fieldValue, field.schema()));
        }
        return record;
    }

    private static Collection<Object> toGenericArray(Collection<?> values, Schema elementSchema) {
        var mapped = new ArrayList<>();
        for (var value : values) {
            mapped.add(toAvroValue(value, elementSchema));
        }
        return mapped;
    }

    private static Map<String, Object> toGenericMap(Map<?, ?> values, Schema valueSchema) {
        var mapped = new HashMap<String, Object>();
        for (var entry : values.entrySet()) {
            mapped.put(String.valueOf(entry.getKey()), toAvroValue(entry.getValue(), valueSchema));
        }
        return mapped;
    }

    private static Object fromAvroValue(Object avroValue, Class<?> targetType) {
        if (avroValue == null) {
            return null;
        }
        if (targetType.isInstance(avroValue)) {
            return avroValue;
        }
        if (targetType == Instant.class && avroValue instanceof Number millis) {
            return Instant.ofEpochMilli(millis.longValue());
        }
        if (targetType == Instant.class && avroValue instanceof GenericRecord record) {
            return fromInstantRecord(record);
        }
        if (targetType == String.class) {
            return avroValue.toString();
        }
        if (targetType.isEnum() && avroValue != null) {
            return Enum.valueOf((Class<? extends Enum>) targetType, avroValue.toString());
        }
        if (targetType.isRecord() && avroValue instanceof GenericRecord record) {
            return instantiateRecord(targetType, record);
        }
        return avroValue;
    }

    private static Object instantiateRecord(Class<?> recordType, GenericRecord record) {
        var components = recordType.getRecordComponents();
        var constructorTypes = new Class<?>[components.length];
        var arguments = new Object[components.length];
        for (var i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            constructorTypes[i] = component.getType();
            arguments[i] = fromAvroValue(record.get(component.getName()), component.getType());
        }
        try {
            var constructor = recordType.getDeclaredConstructor(constructorTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate key record type " + recordType.getName(), e);
        }
    }

    private static Schema resolveConcreteSchemaForValue(Schema schema, Object value) {
        if (schema.getType() != Schema.Type.UNION) {
            return schema;
        }
        return schema.getTypes().stream()
                .filter(candidate -> candidate.getType() != Schema.Type.NULL)
                .filter(candidate -> schemaMatchesValue(candidate, value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No matching union branch found for value type " + value.getClass().getName()));
    }

    private static boolean schemaMatchesValue(Schema schema, Object value) {
        return switch (schema.getType()) {
            case RECORD -> true;
            case ENUM -> value instanceof Enum<?>;
            case ARRAY -> value instanceof Collection<?>;
            case MAP -> value instanceof Map<?, ?>;
            case STRING -> value instanceof CharSequence || value instanceof Instant;
            case LONG -> value instanceof Long || value instanceof Integer || value instanceof Instant;
            case INT -> value instanceof Integer;
            case DOUBLE -> value instanceof Double || value instanceof Float;
            case FLOAT -> value instanceof Float;
            case BOOLEAN -> value instanceof Boolean;
            default -> true;
        };
    }

    private static Object extractFieldValue(Object source, String fieldName) {
        if (source.getClass().isRecord()) {
            for (var component : source.getClass().getRecordComponents()) {
                if (!component.getName().equals(fieldName)) {
                    continue;
                }
                try {
                    return component.getAccessor().invoke(source);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to read record component " + fieldName + " from " + source.getClass().getName(), e);
                }
            }
        }
        try {
            var field = source.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(source);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read field " + fieldName + " from " + source.getClass().getName(), e);
        }
    }

    private static GenericRecord toInstantRecord(Instant instant, Schema recordSchema) {
        var record = new GenericData.Record(recordSchema);
        putIfPresent(record, "seconds", instant.getEpochSecond());
        putIfPresent(record, "epochSecond", instant.getEpochSecond());
        putIfPresent(record, "nanos", instant.getNano());
        putIfPresent(record, "nano", instant.getNano());
        return record;
    }

    private static Instant fromInstantRecord(GenericRecord record) {
        var seconds = numericField(record, "seconds", "epochSecond");
        var nanos = numericField(record, "nanos", "nano");
        return Instant.ofEpochSecond(seconds, nanos);
    }

    private static void putIfPresent(GenericRecord record, String fieldName, Object value) {
        if (record.getSchema().getField(fieldName) != null) {
            record.put(fieldName, value);
        }
    }

    private static long numericField(GenericRecord record, String... candidates) {
        for (var candidate : candidates) {
            var field = record.getSchema().getField(candidate);
            if (field == null) {
                continue;
            }
            var value = record.get(candidate);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        throw new IllegalArgumentException("Missing numeric Instant field in Avro record schema: " + record.getSchema());
    }
}



