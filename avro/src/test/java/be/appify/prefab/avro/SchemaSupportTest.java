package be.appify.prefab.avro;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SchemaSupport")
class SchemaSupportTest {

    @Nested
    @DisplayName("createLogicalSchema")
    class CreateLogicalSchemaTest {

        @Test
        @DisplayName("should create STRING schema with reference logical type")
        void shouldCreateStringWithReference() {
            LogicalType reference = new LogicalType("reference");
            Schema schema = SchemaSupport.createLogicalSchema(Schema.Type.STRING, reference);

            assertThat(schema.getType()).isEqualTo(Schema.Type.STRING);
            assertThat(schema.getLogicalType()).isEqualTo(reference);
            assertThat(schema.getLogicalType().getName()).isEqualTo("reference");
        }

        @Test
        @DisplayName("should create LONG schema with duration-millis logical type")
        void shouldCreateLongWithDurationMillis() {
            LogicalType durationMillis = new LogicalType("duration-millis");
            Schema schema = SchemaSupport.createLogicalSchema(Schema.Type.LONG, durationMillis);

            assertThat(schema.getType()).isEqualTo(Schema.Type.LONG);
            assertThat(schema.getLogicalType()).isEqualTo(durationMillis);
        }

        @Test
        @DisplayName("should create UUID schema")
        void shouldCreateUuidSchema() {
            Schema schema = SchemaSupport.createLogicalSchema(Schema.Type.STRING, LogicalTypes.uuid());
            assertThat(schema.getLogicalType().getName()).isEqualTo("uuid");
        }
    }

    @Nested
    @DisplayName("createNullableSchema")
    class CreateNullableSchemaTest {

        @Test
        @DisplayName("should wrap STRING schema with null")
        void shouldWrapStringWithNull() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema nullable = SchemaSupport.createNullableSchema(stringSchema);

            assertThat(nullable.getType()).isEqualTo(Schema.Type.UNION);
            assertThat(nullable.getTypes()).hasSize(2);
            assertThat(nullable.getTypes()).anySatisfy(t -> assertThat(t.getType()).isEqualTo(Schema.Type.NULL));
            assertThat(nullable.getTypes()).anySatisfy(t -> assertThat(t.getType()).isEqualTo(Schema.Type.STRING));
        }

        @Test
        @DisplayName("should wrap RECORD schema with null")
        void shouldWrapRecordWithNull() {
            Schema recordSchema = Schema.createRecord("TestRecord", null, "namespace", false);
            Schema nullable = SchemaSupport.createNullableSchema(recordSchema);

            assertThat(nullable.getType()).isEqualTo(Schema.Type.UNION);
            assertThat(nullable.getTypes()).hasSize(2);
            assertThat(nullable.getTypes()).anySatisfy(t -> assertThat(t.getType()).isEqualTo(Schema.Type.NULL));
        }

        @Test
        @DisplayName("should not add duplicate null to existing union")
        void shouldNotAddDuplicateNull() {
            Schema innerSchema = Schema.create(Schema.Type.STRING);
            Schema existingUnion = Schema.createUnion(Schema.create(Schema.Type.NULL), innerSchema);
            Schema nullable = SchemaSupport.createNullableSchema(existingUnion);

            assertThat(nullable.getType()).isEqualTo(Schema.Type.UNION);
            assertThat(nullable.getTypes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("arraySchemaOf")
    class ArraySchemaOfTest {

        @Test
        @DisplayName("should return plain array schema unchanged")
        void shouldReturnPlainArray() {
            Schema elementSchema = Schema.create(Schema.Type.STRING);
            Schema arraySchema = Schema.createArray(elementSchema);
            Schema result = SchemaSupport.arraySchemaOf(arraySchema);

            assertThat(result).isSameAs(arraySchema);
            assertThat(result.getType()).isEqualTo(Schema.Type.ARRAY);
        }

        @Test
        @DisplayName("should unwrap array from nullable union")
        void shouldUnwrapArrayFromUnion() {
            Schema elementSchema = Schema.create(Schema.Type.STRING);
            Schema arraySchema = Schema.createArray(elementSchema);
            Schema nullableUnion = Schema.createUnion(Schema.create(Schema.Type.NULL), arraySchema);

            Schema result = SchemaSupport.arraySchemaOf(nullableUnion);

            assertThat(result).isSameAs(arraySchema);
            assertThat(result.getType()).isEqualTo(Schema.Type.ARRAY);
        }

        @Test
        @DisplayName("should throw when union does not contain array")
        void shouldThrowWhenNoArrayInUnion() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), stringSchema);

            assertThatThrownBy(() -> SchemaSupport.arraySchemaOf(union))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No ARRAY schema found in union");
        }
    }

    @Nested
    @DisplayName("enumSchemaOf")
    class EnumSchemaOfTest {

        @Test
        @DisplayName("should return plain enum schema unchanged")
        void shouldReturnPlainEnum() {
            Schema enumSchema = Schema.createEnum("Status", null, "com.example", List.of("ACTIVE", "INACTIVE"));
            Schema result = SchemaSupport.enumSchemaOf(enumSchema);

            assertThat(result).isSameAs(enumSchema);
        }

        @Test
        @DisplayName("should unwrap enum from nullable union")
        void shouldUnwrapEnumFromUnion() {
            Schema enumSchema = Schema.createEnum("Status", null, "com.example", List.of("ACTIVE", "INACTIVE"));
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), enumSchema);

            Schema result = SchemaSupport.enumSchemaOf(union);

            assertThat(result).isSameAs(enumSchema);
        }
    }

    @Nested
    @DisplayName("withSample")
    class WithSampleTest {

        @Test
        @DisplayName("should add sample property to field")
        void shouldAddSampleProperty() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema.Field field = new Schema.Field("name", stringSchema, "The name", null);

            Schema.Field result = SchemaSupport.withSample(field, "John Doe");

            assertThat(result).isSameAs(field);
            assertThat(result.getProp("sample")).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should return same field instance")
        void shouldReturnSameField() {
            Schema.Field field = new Schema.Field("name", Schema.create(Schema.Type.STRING), "", null);
            assertThat(SchemaSupport.withSample(field, "sample")).isSameAs(field);
        }
    }

    @Nested
    @DisplayName("namedTypeOf")
    class NamedTypeOfTest {

        @Test
        @DisplayName("should find named record in flat schema")
        void shouldFindNamedRecord() {
            Schema schema = Schema.createRecord("User", null, "com.example", false);

            Schema result = SchemaSupport.namedTypeOf(schema, "User");
            assertThat(result.getName()).isEqualTo("User");
        }

        @Test
        @DisplayName("should find record nested in another record")
        void shouldFindNestedRecord() {
            Schema addressSchema = Schema.createRecord("Address", null, "com.example", false);
            Schema.Field addressField = new Schema.Field("address", addressSchema, null, null);

            Schema userSchema = Schema.createRecord("User", null, "com.example", false);
            userSchema.setFields(List.of(addressField));

            Schema userUnion = Schema.createUnion(Schema.create(Schema.Type.NULL), userSchema);
            Schema rootSchema = Schema.createRecord("Root", null, "com.example", false);
            var rootField = new Schema.Field("user", userUnion, null, null);
            rootSchema.setFields(List.of(rootField));

            Schema result = SchemaSupport.namedTypeOf(rootSchema, "Address");
            assertThat(result.getName()).isEqualTo("Address");
        }

        @Test
        @DisplayName("should find named enum")
        void shouldFindNamedEnum() {
            Schema enumSchema = Schema.createEnum("Status", null, "com.example", List.of("ACTIVE"));
            Schema result = SchemaSupport.namedTypeOf(enumSchema, "Status");
            assertThat(result.getName()).isEqualTo("Status");
        }

        @Test
        @DisplayName("should find named type inside array")
        void shouldFindInArray() {
            Schema innerRecord = Schema.createRecord("Item", null, "com.example", false);
            Schema arraySchema = Schema.createArray(innerRecord);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), arraySchema);
            Schema root = Schema.createRecord("Root", null, "com.example", false);
            var field = new Schema.Field("items", union, null, null);
            root.setFields(List.of(field));

            Schema result = SchemaSupport.namedTypeOf(root, "Item");
            assertThat(result.getName()).isEqualTo("Item");
        }

        @Test
        @DisplayName("should find named type inside union")
        void shouldFindInUnion() {
            Schema firstRecord = Schema.createRecord("First", null, "com.example", false, emptyList());
            Schema secondRecord = Schema.createRecord("Second", null, "com.example", false, emptyList());
            Schema union = Schema.createUnion(firstRecord, secondRecord);

            Schema result = SchemaSupport.namedTypeOf(union, "Second");
            assertThat(result.getName()).isEqualTo("Second");
        }

        @Test
        @DisplayName("should throw when named type not found")
        void shouldThrowWhenNotFound() {
            Schema schema = Schema.createRecord("User", null, "com.example", false, emptyList());

            assertThatThrownBy(() -> SchemaSupport.namedTypeOf(schema, "NotFound"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No named type 'NotFound' found");
        }

        @Test
        @DisplayName("should not infinite loop on circular references")
        void shouldHandleCircularReference() {
            var parentRecord = Schema.createRecord("Parent", null, "com.example", false);
            var childSchema = Schema.createRecord("Child", null, "com.example", false);
            parentRecord.setFields(List.of(new Schema.Field("child", childSchema, null, null)));

            var grandchildRecord = Schema.createRecord("GrandChild", null, "com.example", false, emptyList());
            childSchema.setFields(List.of(new Schema.Field("grandchild", grandchildRecord, null, null)));

            // This should not throw
            assertThatCode(() -> SchemaSupport.namedTypeOf(parentRecord, "GrandChild"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("recordSchemaOf")
    class RecordSchemaOfTest {

        @Test
        @DisplayName("should return plain record schema")
        void shouldReturnPlainRecord() {
            Schema recordSchema = Schema.createRecord("User", null, "com.example", false);
            Schema result = SchemaSupport.recordSchemaOf(recordSchema);
            assertThat(result).isSameAs(recordSchema);
        }

        @Test
        @DisplayName("should unwrap record from union")
        void shouldUnwrapFromUnion() {
            Schema recordSchema = Schema.createRecord("User", null, "com.example", false);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), recordSchema);

            Schema result = SchemaSupport.recordSchemaOf(union);
            assertThat(result).isSameAs(recordSchema);
        }

        @Test
        @DisplayName("should throw when union does not contain record")
        void shouldThrowWhenNoRecordInUnion() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), stringSchema);

            assertThatThrownBy(() -> SchemaSupport.recordSchemaOf(union))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No RECORD schema found in union");
        }
    }

    @Nested
    @DisplayName("isRecordSchema")
    class IsRecordSchemaTest {

        @Test
        @DisplayName("should return true for plain record")
        void shouldReturnTrueForPlainRecord() {
            Schema recordSchema = Schema.createRecord("User", null, "com.example", false);
            assertThat(SchemaSupport.isRecordSchema(recordSchema)).isTrue();
        }

        @Test
        @DisplayName("should return true for record in union")
        void shouldReturnTrueForRecordInUnion() {
            Schema recordSchema = Schema.createRecord("User", null, "com.example", false);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), recordSchema);
            assertThat(SchemaSupport.isRecordSchema(union)).isTrue();
        }

        @Test
        @DisplayName("should return false for string schema")
        void shouldReturnFalseForString() {
            assertThat(SchemaSupport.isRecordSchema(Schema.create(Schema.Type.STRING))).isFalse();
        }

        @Test
        @DisplayName("should return false for union without record")
        void shouldReturnFalseForUnionWithoutRecord() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), stringSchema);
            assertThat(SchemaSupport.isRecordSchema(union)).isFalse();
        }
    }

    @Nested
    @DisplayName("singleValueRecord")
    class SingleValueRecordTest {

        @Test
        @DisplayName("should create record with single field value")
        void shouldCreateRecord() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema.Field field = new Schema.Field("name", stringSchema, null, null);
            Schema recordSchema = Schema.createRecord("TestRecord", null, "com.example", false);
            recordSchema.setFields(List.of(field));

            GenericRecord record = SchemaSupport.singleValueRecord(recordSchema, "name", "John");

            assertThat(record.get("name")).isEqualTo("John");
        }

        @Test
        @DisplayName("should unwrap record from union before creating record")
        void shouldUnwrapFromUnion() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema.Field field = new Schema.Field("value", stringSchema, null, null);
            Schema recordSchema = Schema.createRecord("TestRecord", null, "com.example", false);
            recordSchema.setFields(List.of(field));

            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), recordSchema);

            GenericRecord record = SchemaSupport.singleValueRecord(union, "value", "test");

            assertThat(record.get("value")).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("createNullableUnion")
    class CreateNullableUnionTest {

        @Test
        @DisplayName("should wrap non-union schema with null")
        void shouldWrapNonUnionSchema() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema result = SchemaSupport.createNullableUnion(stringSchema);

            assertThat(result.getType()).isEqualTo(Schema.Type.UNION);
            assertThat(result.getTypes()).hasSize(2);
            assertThat(result.getTypes()).anySatisfy(t -> assertThat(t.getType()).isEqualTo(Schema.Type.NULL));
        }

        @Test
        @DisplayName("should add null to union without null")
        void shouldAddNullToUnion() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema enumSchema = Schema.createEnum("Status", null, "com.example", List.of("A", "B"));
            Schema union = Schema.createUnion(stringSchema, enumSchema);

            Schema result = SchemaSupport.createNullableUnion(union);

            assertThat(result.getType()).isEqualTo(Schema.Type.UNION);
            assertThat(result.getTypes()).hasSize(3);
            assertThat(result.getTypes()).anySatisfy(t -> assertThat(t.getType()).isEqualTo(Schema.Type.NULL));
        }

        @Test
        @DisplayName("should not add duplicate null to union with null")
        void shouldNotAddDuplicateNull() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema existingUnion = Schema.createUnion(Schema.create(Schema.Type.NULL), stringSchema);

            Schema result = SchemaSupport.createNullableUnion(existingUnion);

            assertThat(result.getType()).isEqualTo(Schema.Type.UNION);
            assertThat(result.getTypes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("namedBranchOf")
    class NamedBranchOfTest {

        @Test
        @DisplayName("should find branch by name in union")
        void shouldFindBranchByName() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema enumSchema = Schema.createEnum("Status", null, "com.example", List.of("A", "B"));
            Schema union = Schema.createUnion(stringSchema, enumSchema);

            Schema result = SchemaSupport.namedBranchOf(union, "Status");
            assertThat(result.getName()).isEqualTo("Status");
        }

        @Test
        @DisplayName("should throw when branch not found")
        void shouldThrowWhenBranchNotFound() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);
            Schema union = Schema.createUnion(Schema.create(Schema.Type.NULL), stringSchema);

            assertThatThrownBy(() -> SchemaSupport.namedBranchOf(union, "Missing"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No branch named 'Missing' in union");
        }

        @Test
        @DisplayName("should return schema when schema name matches branch name")
        void shouldReturnWhenNameMatches() {
            Schema enumSchema = Schema.createEnum("Status", null, "com.example", List.of("A", "B"));

            Schema result = SchemaSupport.namedBranchOf(enumSchema, "Status");
            assertThat(result).isSameAs(enumSchema);
        }

        @Test
        @DisplayName("should throw when schema is not a union and name does not match")
        void shouldThrowWhenNotUnionAndNameMismatch() {
            Schema stringSchema = Schema.create(Schema.Type.STRING);

            assertThatThrownBy(() -> SchemaSupport.namedBranchOf(stringSchema, "Status"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Schema is not a union and does not have branch");
        }
    }

    @Nested
    @DisplayName("getField")
    class GetFieldTest {

        private Schema recordSchema(String... fieldNames) {
            var fields = java.util.Arrays.stream(fieldNames)
                    .map(name -> new Schema.Field(name, Schema.create(Schema.Type.STRING)))
                    .toList();
            return Schema.createRecord("TestRecord", null, "com.example", false, fields);
        }

        @Test
        @DisplayName("should return the field value when the field exists in the schema")
        void shouldReturnValueWhenFieldExists() {
            var schema = recordSchema("name");
            var record = new org.apache.avro.generic.GenericData.Record(schema);
            record.put("name", "Alice");

            assertThat(SchemaSupport.getField(record, "name")).isEqualTo("Alice");
        }

        @Test
        @DisplayName("should return null when the field is not present in the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = recordSchema("name");
            var record = new org.apache.avro.generic.GenericData.Record(schema);
            record.put("name", "Alice");

            assertThat(SchemaSupport.getField(record, "addedLater")).isNull();
        }

        @Test
        @DisplayName("should return null when the field value is explicitly null")
        void shouldReturnNullWhenFieldValueIsNull() {
            var nullableField = new Schema.Field("description",
                    Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                    null, Schema.Field.NULL_DEFAULT_VALUE);
            var schema = Schema.createRecord("TestRecord", null, "com.example", false, List.of(nullableField));
            var record = new org.apache.avro.generic.GenericData.Record(schema);
            record.put("description", null);

            assertThat(SchemaSupport.getField(record, "description")).isNull();
        }
    }

    @Nested
    @DisplayName("getString")
    class GetStringTest {

        @Test
        @DisplayName("should return the field value as a String")
        void shouldReturnStringValue() {
            var field = new Schema.Field("name", Schema.create(Schema.Type.STRING));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("name", "Alice");

            assertThat(SchemaSupport.getString(record, "name")).isEqualTo("Alice");
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getString(record, "missing")).isNull();
        }

        @Test
        @DisplayName("should return null when the field value is null")
        void shouldReturnNullWhenFieldValueIsNull() {
            var nullableField = new Schema.Field("name",
                    Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                    null, Schema.Field.NULL_DEFAULT_VALUE);
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(nullableField));
            var record = new GenericData.Record(schema);
            record.put("name", null);

            assertThat(SchemaSupport.getString(record, "name")).isNull();
        }
    }

    @Nested
    @DisplayName("getInteger")
    class GetIntegerTest {

        @Test
        @DisplayName("should return the field value as an Integer")
        void shouldReturnIntegerValue() {
            var field = new Schema.Field("count", Schema.create(Schema.Type.INT));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("count", 42);

            assertThat(SchemaSupport.getInteger(record, "count")).isEqualTo(42);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getInteger(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getLong")
    class GetLongTest {

        @Test
        @DisplayName("should return the field value as a Long")
        void shouldReturnLongValue() {
            var field = new Schema.Field("ts", Schema.create(Schema.Type.LONG));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("ts", 1_000_000L);

            assertThat(SchemaSupport.getLong(record, "ts")).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getLong(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getDouble")
    class GetDoubleTest {

        @Test
        @DisplayName("should return the field value as a Double")
        void shouldReturnDoubleValue() {
            var field = new Schema.Field("score", Schema.create(Schema.Type.DOUBLE));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("score", 3.14);

            assertThat(SchemaSupport.getDouble(record, "score")).isEqualTo(3.14);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getDouble(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getFloat")
    class GetFloatTest {

        @Test
        @DisplayName("should return the field value as a Float")
        void shouldReturnFloatValue() {
            var field = new Schema.Field("ratio", Schema.create(Schema.Type.FLOAT));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("ratio", 1.5f);

            assertThat(SchemaSupport.getFloat(record, "ratio")).isEqualTo(1.5f);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getFloat(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getBoolean")
    class GetBooleanTest {

        @Test
        @DisplayName("should return the field value as a Boolean")
        void shouldReturnBooleanValue() {
            var field = new Schema.Field("active", Schema.create(Schema.Type.BOOLEAN));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("active", true);

            assertThat(SchemaSupport.getBoolean(record, "active")).isTrue();
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getBoolean(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getInstant")
    class GetInstantTest {

        @Test
        @DisplayName("should convert epoch-millis long to Instant")
        void shouldConvertEpochMillisToInstant() {
            var field = new Schema.Field("ts", Schema.create(Schema.Type.LONG));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("ts", 1_000L);

            assertThat(SchemaSupport.getInstant(record, "ts")).isEqualTo(Instant.ofEpochMilli(1_000L));
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getInstant(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getLocalDate")
    class GetLocalDateTest {

        @Test
        @DisplayName("should convert epoch-day int to LocalDate")
        void shouldConvertEpochDayToLocalDate() {
            var field = new Schema.Field("date", Schema.create(Schema.Type.INT));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("date", 1);

            assertThat(SchemaSupport.getLocalDate(record, "date")).isEqualTo(LocalDate.ofEpochDay(1));
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getLocalDate(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getDuration")
    class GetDurationTest {

        @Test
        @DisplayName("should convert millis long to Duration")
        void shouldConvertMillisToDuration() {
            var field = new Schema.Field("dur", Schema.create(Schema.Type.LONG));
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("dur", 5_000L);

            assertThat(SchemaSupport.getDuration(record, "dur")).isEqualTo(Duration.ofMillis(5_000L));
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getDuration(record, "missing")).isNull();
        }
    }

    @Nested
    @DisplayName("getRecord")
    class GetRecordTest {

        private GenericData.Record nestedRecord() {
            var nested = Schema.createRecord("Nested", null, "com.example", false,
                    List.of(new Schema.Field("val", Schema.create(Schema.Type.STRING))));
            var record = new GenericData.Record(nested);
            record.put("val", "hello");
            return record;
        }

        @Test
        @DisplayName("should return the nested GenericRecord when present")
        void shouldReturnNestedRecord() {
            var nested = nestedRecord();
            var nestedField = new Schema.Field("child", nested.getSchema());
            var schema = Schema.createRecord("Parent", null, "com.example", false, List.of(nestedField));
            var parent = new GenericData.Record(schema);
            parent.put("child", nested);

            assertThat(SchemaSupport.getRecord(parent, "child")).isSameAs(nested);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getRecord(record, "missing")).isNull();
        }

        @Test
        @DisplayName("functional variant should apply the converter when the record is present")
        void functionalVariantShouldApplyConverter() {
            var nested = nestedRecord();
            var nestedField = new Schema.Field("child", nested.getSchema());
            var schema = Schema.createRecord("Parent", null, "com.example", false, List.of(nestedField));
            var parent = new GenericData.Record(schema);
            parent.put("child", nested);

            var result = SchemaSupport.getRecord(parent, "child", r -> r.get("val").toString());

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("functional variant should return null without calling the converter when field is absent")
        void functionalVariantShouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);
            var converterCalled = new boolean[]{false};

            var result = SchemaSupport.<String>getRecord(record, "missing", r -> {
                converterCalled[0] = true;
                return "should-not-be-called";
            });

            assertThat(result).isNull();
            assertThat(converterCalled[0]).isFalse();
        }
    }

    @Nested
    @DisplayName("getEnum")
    class GetEnumTest {

        enum Color { RED, GREEN, BLUE }

        @Test
        @DisplayName("should return the matching enum constant")
        void shouldReturnEnumConstant() {
            var enumSchema = Schema.createEnum("Color", null, "com.example", List.of("RED", "GREEN", "BLUE"));
            var field = new Schema.Field("color", enumSchema);
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            record.put("color", new GenericData.EnumSymbol(enumSchema, "GREEN"));

            assertThat(SchemaSupport.getEnum(record, "color", Color.class)).isEqualTo(Color.GREEN);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            assertThat(SchemaSupport.getEnum(record, "missing", Color.class)).isNull();
        }
    }

    @Nested
    @DisplayName("getArray")
    class GetArrayTest {

        @Test
        @DisplayName("should return the GenericData.Array when present")
        void shouldReturnArray() {
            var arraySchema = Schema.createArray(Schema.create(Schema.Type.STRING));
            var field = new Schema.Field("tags", arraySchema);
            var schema = Schema.createRecord("R", null, "com.example", false, List.of(field));
            var record = new GenericData.Record(schema);
            var array = new GenericData.Array<>(arraySchema, List.of("a", "b"));
            record.put("tags", array);

            GenericData.Array<?> result = SchemaSupport.getArray(record, "tags");
            assertThat((Object) result).isSameAs(array);
        }

        @Test
        @DisplayName("should return null when the field is absent from the schema")
        void shouldReturnNullWhenFieldAbsent() {
            var schema = Schema.createRecord("R", null, "com.example", false, emptyList());
            var record = new GenericData.Record(schema);

            GenericData.Array<?> result = SchemaSupport.getArray(record, "missing");
            assertThat((Object) result).isNull();
        }
    }
}
