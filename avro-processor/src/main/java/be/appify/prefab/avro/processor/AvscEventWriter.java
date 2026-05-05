package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.Namespace;
import be.appify.prefab.processor.BuilderWriter;
import be.appify.prefab.processor.JavaFileWriter;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import org.apache.avro.Schema;

class AvscEventWriter {

    private static final String PROP_SAMPLE = "sample";
    private static final String PROP_LOGICAL_TYPE = "logicalType";
    private static final String LOGICAL_TYPE_TIMESTAMP_MILLIS = "timestamp-millis";
    private static final String LOGICAL_TYPE_DATE = "date";
    private static final String LOGICAL_TYPE_DURATION_MILLIS = "duration-millis";

    private static final String OPTION_SETTER_PREFIX = "prefab.builder.setterPrefix";
    private static final String DEFAULT_SETTER_PREFIX = "with";

    private final ProcessingEnvironment processingEnvironment;

    AvscEventWriter(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    void writeAll(Schema schema, String topic, Event.Platform platform, String defaultPackage, ClassName contractInterface) {
        var namedTypes = collectNamedTypes(schema);
        var fileWriter = new JavaFileWriter(processingEnvironment, "");

        writeTopLevelRecord(schema, topic, platform, defaultPackage, contractInterface, fileWriter);
        writeNestedTypes(schema, namedTypes, defaultPackage, fileWriter);
    }

    private void writeTopLevelRecord(Schema schema, String topic, Event.Platform platform,
            String defaultPackage, ClassName contractInterface, JavaFileWriter fileWriter) {
        var topLevelSpec = buildTopLevelRecord(schema, topic, platform, defaultPackage, contractInterface);
        if (topLevelSpec != null) {
            fileWriter.writeFile(defaultPackage, schema.getName(), topLevelSpec);
        }
    }

    private void writeNestedTypes(Schema topLevelSchema, Map<String, Schema> namedTypes,
            String defaultPackage, JavaFileWriter fileWriter) {
        for (var entry : namedTypes.entrySet()) {
            var namedSchema = entry.getValue();
            if (namedSchema.equals(topLevelSchema)) continue;
            writeNestedType(namedSchema, defaultPackage, fileWriter);
        }
    }

    private void writeNestedType(Schema schema, String defaultPackage, JavaFileWriter fileWriter) {
        if (schema.getType() == Schema.Type.RECORD) {
            var spec = buildNestedRecord(schema, defaultPackage);
            if (spec != null) {
                fileWriter.writeFile(defaultPackage, schema.getName(), spec);
            }
        } else if (schema.getType() == Schema.Type.ENUM) {
            fileWriter.writeFile(defaultPackage, schema.getName(), buildEnum(schema));
        }
    }

    private Map<String, Schema> collectNamedTypes(Schema schema) {
        var collected = new LinkedHashMap<String, Schema>();
        collectNamedTypesInto(schema, collected);
        return collected;
    }

    private void collectNamedTypesInto(Schema schema, Map<String, Schema> collected) {
        if (schema == null) return;
        switch (schema.getType()) {
            case RECORD -> {
                if (collected.containsKey(schema.getFullName())) return;
                collected.put(schema.getFullName(), schema);
                schema.getFields().forEach(field -> collectNamedTypesInto(field.schema(), collected));
            }
            case ENUM -> collected.putIfAbsent(schema.getFullName(), schema);
            case ARRAY -> collectNamedTypesInto(schema.getElementType(), collected);
            case UNION -> schema.getTypes().forEach(t -> collectNamedTypesInto(t, collected));
            default -> { /* primitives and logical types need no traversal */ }
        }
    }

    private TypeSpec buildTopLevelRecord(Schema schema, String topic, Event.Platform platform,
            String schemaPackage, ClassName contractInterface) {
        return buildFields(schema, schemaPackage)
                .map(fields -> {
                    var recordType = ClassName.get(schemaPackage, schema.getName());
                    var builder = TypeSpec.recordBuilder(schema.getName())
                            .addModifiers(Modifier.PUBLIC)
                            .recordConstructor(MethodSpec.compactConstructorBuilder().addParameters(fields).build())
                            .addAnnotation(buildEventAnnotation(topic, platform))
                            .addSuperinterface(contractInterface);
                    docOf(schema).ifPresent(doc -> builder.addAnnotation(docAnnotation(doc)));
                    namespaceAnnotation(schema).ifPresent(builder::addAnnotation);
                    new BuilderWriter(builderSetterPrefix()).enrichWithBuilder(builder, recordType, strippedParams(fields));
                    return builder.build();
                })
                .orElse(null);
    }

    private TypeSpec buildNestedRecord(Schema schema, String defaultPackage) {
        return buildFields(schema, defaultPackage)
                .map(fields -> {
                    var recordType = ClassName.get(defaultPackage, schema.getName());
                    var builder = TypeSpec.recordBuilder(schema.getName())
                            .addModifiers(Modifier.PUBLIC)
                            .recordConstructor(MethodSpec.compactConstructorBuilder().addParameters(fields).build());
                    docOf(schema).ifPresent(doc -> builder.addAnnotation(docAnnotation(doc)));
                    namespaceAnnotation(schema).ifPresent(builder::addAnnotation);
                    new BuilderWriter(builderSetterPrefix()).enrichWithBuilder(builder, recordType, strippedParams(fields));
                    return builder.build();
                })
                .orElse(null);
    }

    private List<ParameterSpec> strippedParams(List<ParameterSpec> fields) {
        return fields.stream()
                .map(f -> ParameterSpec.builder(f.type(), f.name()).build())
                .toList();
    }

    private Optional<List<ParameterSpec>> buildFields(Schema schema, String defaultPackage) {
        var fields = new ArrayList<ParameterSpec>();
        for (var field : schema.getFields()) {
            var spec = buildField(field, defaultPackage);
            if (spec == null) return Optional.empty();
            fields.add(spec);
        }
        return Optional.of(fields);
    }

    private ParameterSpec buildField(Schema.Field field, String defaultPackage) {
        var resolution = resolveFieldSchema(field);
        if (resolution == null) return null;

        var typeName = toTypeName(resolution.schema(), defaultPackage, resolution.nullable());
        if (typeName == null) return null;

        var paramBuilder = ParameterSpec.builder(typeName, field.name());
        if (resolution.nullable()) {
            paramBuilder.addAnnotation(Nullable.class);
        }
        sampleOf(field).ifPresent(sample -> paramBuilder.addAnnotation(exampleAnnotation(sample)));
        docOf(field).ifPresent(doc -> paramBuilder.addAnnotation(docAnnotation(doc)));
        return paramBuilder.build();
    }

    private FieldResolution resolveFieldSchema(Schema.Field field) {
        var fieldSchema = field.schema();
        if (fieldSchema.getType() != Schema.Type.UNION) {
            return new FieldResolution(fieldSchema, false);
        }
        return resolveUnionSchema(field, fieldSchema);
    }

    private FieldResolution resolveUnionSchema(Schema.Field field, Schema unionSchema) {
        var nonNullTypes = unionSchema.getTypes().stream()
                .filter(t -> t.getType() != Schema.Type.NULL)
                .toList();
        if (nonNullTypes.size() == 1) {
            return new FieldResolution(nonNullTypes.getFirst(), true);
        }
        reportError("Unsupported union type for field '" + field.name()
                + "': only [\"null\", T] unions are supported.");
        return null;
    }

    private java.util.Optional<String> sampleOf(Schema.Field field) {
        var fieldLevelSample = field.getProp(PROP_SAMPLE);
        if (fieldLevelSample != null) return java.util.Optional.of(fieldLevelSample);

        var schemaLevelSample = field.schema().getProp(PROP_SAMPLE);
        if (schemaLevelSample != null) return java.util.Optional.of(schemaLevelSample);

        if (field.schema().getType() == Schema.Type.UNION) {
            var nonNull = field.schema().getTypes().stream()
                    .filter(t -> t.getType() != Schema.Type.NULL)
                    .findFirst();
            return nonNull.map(s -> s.getProp(PROP_SAMPLE));
        }
        return java.util.Optional.empty();
    }

    private AnnotationSpec exampleAnnotation(String sample) {
        return AnnotationSpec.builder(Example.class)
                .addMember("value", "$S", sample)
                .build();
    }

    private java.util.Optional<String> docOf(Schema.Field field) {
        return java.util.Optional.ofNullable(field.doc());
    }

    private java.util.Optional<String> docOf(Schema schema) {
        return java.util.Optional.ofNullable(schema.getDoc());
    }

    private AnnotationSpec docAnnotation(String doc) {
        return AnnotationSpec.builder(Doc.class)
                .addMember("value", "$S", doc)
                .build();
    }

    private Optional<AnnotationSpec> namespaceAnnotation(Schema schema) {
        return Optional.ofNullable(schema.getNamespace())
                .filter(namespace -> !namespace.isBlank())
                .map(namespace -> AnnotationSpec.builder(Namespace.class)
                        .addMember("value", "$S", namespace)
                        .build());
    }

    private AnnotationSpec buildEventAnnotation(String topic, Event.Platform platform) {
        var builder = AnnotationSpec.builder(Event.class)
                .addMember("topic", "$S", topic)
                .addMember("serialization", "$T.$L", ClassName.get(Event.Serialization.class), "AVRO");
        if (platform != null && platform != Event.Platform.DERIVED) {
            builder.addMember("platform", "$T.$L", ClassName.get(Event.Platform.class), platform.name());
        }
        return builder.build();
    }

    private TypeSpec buildEnum(Schema schema) {
        var enumBuilder = TypeSpec.enumBuilder(schema.getName())
                .addModifiers(Modifier.PUBLIC);
        namespaceAnnotation(schema).ifPresent(enumBuilder::addAnnotation);
        schema.getEnumSymbols().forEach(enumBuilder::addEnumConstant);
        return enumBuilder.build();
    }

    private TypeName toTypeName(Schema schema, String defaultPackage, boolean nullable) {
        var logicalTypeName = resolveLogicalTypeName(schema);
        if (logicalTypeName != null) {
            return toLogicalTypeName(logicalTypeName);
        }
        return toPrimitiveTypeName(schema, defaultPackage, nullable);
    }

    private String resolveLogicalTypeName(Schema schema) {
        var fromProp = schema.getProp(PROP_LOGICAL_TYPE);
        if (fromProp != null) return fromProp;
        return schema.getLogicalType() != null ? schema.getLogicalType().getName() : null;
    }

    private TypeName toLogicalTypeName(String logicalTypeName) {
        return switch (logicalTypeName) {
            case LOGICAL_TYPE_TIMESTAMP_MILLIS -> ClassName.get(Instant.class);
            case LOGICAL_TYPE_DATE -> ClassName.get(LocalDate.class);
            case LOGICAL_TYPE_DURATION_MILLIS -> ClassName.get(Duration.class);
            default -> {
                reportError("Unsupported logical type: " + logicalTypeName);
                yield null;
            }
        };
    }

    private TypeName toPrimitiveTypeName(Schema schema, String defaultPackage, boolean nullable) {
        return switch (schema.getType()) {
            case STRING -> ClassName.get(String.class);
            case INT -> boxIfNullable(TypeName.INT, nullable);
            case LONG -> boxIfNullable(TypeName.LONG, nullable);
            case DOUBLE -> boxIfNullable(TypeName.DOUBLE, nullable);
            case FLOAT -> boxIfNullable(TypeName.FLOAT, nullable);
            case BOOLEAN -> boxIfNullable(TypeName.BOOLEAN, nullable);
            case ARRAY -> toListTypeName(schema, defaultPackage);
            case RECORD, ENUM -> ClassName.get(defaultPackage, schema.getName());
            default -> {
                reportError("Unsupported Avro type: " + schema.getType());
                yield null;
            }
        };
    }


    private TypeName boxIfNullable(TypeName typeName, boolean nullable) {
        return nullable ? typeName.box() : typeName;
    }

    private TypeName toListTypeName(Schema schema, String defaultPackage) {
        var elementType = toTypeName(schema.getElementType(), defaultPackage, false);
        if (elementType == null) return null;
        var boxed = elementType.isPrimitive() ? elementType.box() : elementType;
        return ParameterizedTypeName.get(ClassName.get(List.class), boxed);
    }

    private String builderSetterPrefix() {
        return processingEnvironment.getOptions().getOrDefault(OPTION_SETTER_PREFIX, DEFAULT_SETTER_PREFIX);
    }

    private void reportError(String message) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    private record FieldResolution(Schema schema, boolean nullable) {}

}
