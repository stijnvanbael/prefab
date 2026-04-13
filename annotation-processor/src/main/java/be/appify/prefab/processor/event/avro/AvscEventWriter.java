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

    void writeAll(Schema schema, String topic, Event.Platform platform, String defaultPackage) {
        var namedTypes = new LinkedHashMap<String, Schema>();
        collectNamedTypes(schema, namedTypes);

        var fileWriter = new JavaFileWriter(processingEnvironment, "");

        // Write the top-level event record with @Event annotation
        var topLevelSpec = buildRecord(schema, topic, platform, defaultPackage, true);
        if (topLevelSpec == null) return;
        var topPackage = packageOf(schema, defaultPackage);
        fileWriter.writeFile(topPackage, schema.getName(), topLevelSpec);

        // Write nested named types (records without @Event, enums)
        for (var entry : namedTypes.entrySet()) {
            var namedSchema = entry.getValue();
            if (namedSchema.equals(schema)) continue;
            var pkg = packageOf(namedSchema, defaultPackage);
            if (namedSchema.getType() == Schema.Type.RECORD) {
                var spec = buildRecord(namedSchema, null, null, defaultPackage, false);
                if (spec != null) {
                    fileWriter.writeFile(pkg, namedSchema.getName(), spec);
                }
            } else if (namedSchema.getType() == Schema.Type.ENUM) {
                var spec = buildEnum(namedSchema);
                fileWriter.writeFile(pkg, namedSchema.getName(), spec);
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
            String defaultPackage, boolean isTopLevel) {
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
                // Box primitives for List (Java generics require object types, not primitives)
                var boxed = elementType.box();
                yield ParameterizedTypeName.get(ClassName.get(List.class), boxed);
            }
            case RECORD -> {
                var pkg = packageOf(schema, defaultPackage);
                yield ClassName.get(pkg, schema.getName());
            }
            case ENUM -> {
                var pkg = packageOf(schema, defaultPackage);
                yield ClassName.get(pkg, schema.getName());
            }
            default -> {
                processingEnvironment.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Unsupported Avro type: " + schema.getType());
                yield null;
            }
        };
    }

    private String packageOf(Schema schema, String defaultPackage) {
        var namespace = schema.getNamespace();
        return (namespace != null && !namespace.isBlank()) ? namespace : defaultPackage;
    }
}
