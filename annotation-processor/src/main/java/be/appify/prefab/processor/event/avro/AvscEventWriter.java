package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.annotations.Event;
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
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import org.apache.avro.Schema;

class AvscEventWriter {

    private final ProcessingEnvironment processingEnvironment;

    AvscEventWriter(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    void writeAll(Schema schema, String topic, Event.Platform platform, String defaultPackage, ClassName contractInterface) {
        var namedTypes = new LinkedHashMap<String, Schema>();
        collectNamedTypes(schema, namedTypes);

        var fileWriter = new JavaFileWriter(processingEnvironment, "");

        // Top-level record lives in the same package as the contract interface it implements
        var topLevelSpec = buildRecord(schema, topic, platform, defaultPackage, true, contractInterface);
        if (topLevelSpec == null) return;
        fileWriter.writeFile(contractInterface.packageName(), schema.getName(), topLevelSpec);

        // Write nested named types (records and enums) — same package as the contract interface
        for (var entry : namedTypes.entrySet()) {
            var namedSchema = entry.getValue();
            if (namedSchema.equals(schema)) continue;
            if (namedSchema.getType() == Schema.Type.RECORD) {
                var spec = buildRecord(namedSchema, null, null, defaultPackage, false, null);
                if (spec != null) {
                    fileWriter.writeFile(defaultPackage, namedSchema.getName(), spec);
                }
            } else if (namedSchema.getType() == Schema.Type.ENUM) {
                var spec = buildEnum(namedSchema);
                fileWriter.writeFile(defaultPackage, namedSchema.getName(), spec);
            }
        }
    }

    private void collectNamedTypes(Schema schema, Map<String, Schema> collected) {
        if (schema == null) return;
        switch (schema.getType()) {
            case RECORD -> {
                if (collected.containsKey(schema.getFullName())) return;
                collected.put(schema.getFullName(), schema);
                for (var field : schema.getFields()) {
                    collectNamedTypes(field.schema(), collected);
                }
            }
            case ENUM -> {
                if (collected.containsKey(schema.getFullName())) return;
                collected.put(schema.getFullName(), schema);
            }
            case ARRAY -> collectNamedTypes(schema.getElementType(), collected);
            case UNION -> schema.getTypes().forEach(t -> collectNamedTypes(t, collected));
            default -> { /* primitives and logical types need no traversal */ }
        }
    }

    private TypeSpec buildRecord(Schema schema, String topic, Event.Platform platform,
            String defaultPackage, boolean isTopLevel, ClassName contractInterface) {
        var fields = new ArrayList<ParameterSpec>();
        for (var field : schema.getFields()) {
            var fieldSchema = field.schema();
            boolean nullable = false;
            Schema effectiveSchema = fieldSchema;

            if (fieldSchema.getType() == Schema.Type.UNION) {
                var nonNullTypes = fieldSchema.getTypes().stream()
                        .filter(t -> t.getType() != Schema.Type.NULL)
                        .toList();
                if (nonNullTypes.size() == 1) {
                    nullable = true;
                    effectiveSchema = nonNullTypes.getFirst();
                } else {
                    processingEnvironment.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "Unsupported union type for field '" + field.name()
                                    + "': only [\"null\", T] unions are supported.");
                    return null;
                }
            }

            var typeName = toTypeName(effectiveSchema, defaultPackage, nullable);
            if (typeName == null) return null;

            var paramBuilder = ParameterSpec.builder(typeName, field.name());
            if (nullable) {
                paramBuilder.addAnnotation(Nullable.class);
            }
            fields.add(paramBuilder.build());
        }

        var recordBuilder = TypeSpec.recordBuilder(schema.getName())
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(fields)
                        .build());

        if (isTopLevel && topic != null) {
            var eventAnnotation = AnnotationSpec.builder(Event.class)
                    .addMember("topic", "$S", topic)
                    .addMember("serialization", "$T.$L",
                            ClassName.get(Event.Serialization.class), "AVRO");
            if (platform != null && platform != Event.Platform.DERIVED) {
                eventAnnotation.addMember("platform", "$T.$L",
                        ClassName.get(Event.Platform.class), platform.name());
            }
            recordBuilder.addAnnotation(eventAnnotation.build());
        }

        if (isTopLevel && contractInterface != null) {
            recordBuilder.addSuperinterface(contractInterface);
        }

        return recordBuilder.build();
    }

    private TypeSpec buildEnum(Schema schema) {
        var enumBuilder = TypeSpec.enumBuilder(schema.getName())
                .addModifiers(Modifier.PUBLIC);
        schema.getEnumSymbols().forEach(enumBuilder::addEnumConstant);
        return enumBuilder.build();
    }

    private TypeName toTypeName(Schema schema, String defaultPackage, boolean nullable) {
        var logicalTypeName = schema.getProp("logicalType");
        if (logicalTypeName == null && schema.getLogicalType() != null) {
            logicalTypeName = schema.getLogicalType().getName();
        }
        if (logicalTypeName != null) {
            return switch (logicalTypeName) {
                case "timestamp-millis" -> ClassName.get(Instant.class);
                case "date" -> ClassName.get(LocalDate.class);
                case "duration-millis" -> ClassName.get(Duration.class);
                default -> {
                    processingEnvironment.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "Unsupported logical type: " + logicalTypeName);
                    yield null;
                }
            };
        }

        return switch (schema.getType()) {
            case STRING -> ClassName.get(String.class);
            case INT -> nullable ? TypeName.INT.box() : TypeName.INT;
            case LONG -> nullable ? TypeName.LONG.box() : TypeName.LONG;
            case DOUBLE -> nullable ? TypeName.DOUBLE.box() : TypeName.DOUBLE;
            case FLOAT -> nullable ? TypeName.FLOAT.box() : TypeName.FLOAT;
            case BOOLEAN -> nullable ? TypeName.BOOLEAN.box() : TypeName.BOOLEAN;
            case ARRAY -> {
                var elementSchema = schema.getElementType();
                var elementType = toTypeName(elementSchema, defaultPackage, false);
                if (elementType == null) yield null;
                // Box primitives for List (Java generics require reference types)
                var boxed = elementType.isPrimitive() ? elementType.box() : elementType;
                yield ParameterizedTypeName.get(ClassName.get(List.class), boxed);
            }
            case RECORD, ENUM -> ClassName.get(defaultPackage, schema.getName());
            default -> {
                processingEnvironment.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Unsupported Avro type: " + schema.getType());
                yield null;
            }
        };
    }

}
