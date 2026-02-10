package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.avro.SchemaSupport;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.avro.AvroPlugin.isLogicalType;
import static be.appify.prefab.processor.event.avro.AvroPlugin.isNestedRecord;
import static be.appify.prefab.processor.event.avro.AvroPlugin.nestedTypes;
import static be.appify.prefab.processor.event.avro.AvroPlugin.sealedSubtypes;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class EventSchemaFactoryWriter {
    void writeSchemaFactory(TypeManifest event, PrefabContext context) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.avro");

        var name = "%sSchemaFactory".formatted(event.simpleName().replace(".", ""));
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build());
        type.addMethod(constructor(event, context))
                .addMethod(createSchemaMethod());

        fileWriter.writeFile(event.packageName(), name, type.build());

    }

    private static MethodSpec constructor(TypeManifest event, PrefabContext context) {
        var constructor = getBuilder();
        nestedTypes(List.of(event)).forEach(nestedType -> addSchemaFactory(nestedType, constructor));
        sealedSubtypes(List.of(event)).forEach(subtype -> addSchemaFactory(subtype, constructor));
        return constructor
                .addStatement("this.schema = $L", createSchema(event, context))
                .build();
    }

    private static void addSchemaFactory(TypeManifest nestedType, MethodSpec.Builder constructor) {
        var schemaFactoryName = uncapitalize("%sSchemaFactory".formatted(nestedType.simpleName().replace(".", "")));
        var schemaFactoryType = ClassName.get(nestedType.packageName() + ".infrastructure.avro", capitalize(schemaFactoryName));
        constructor.addParameter(schemaFactoryType, schemaFactoryName);
    }

    private static MethodSpec.Builder getBuilder() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
    }

    private static MethodSpec createSchemaMethod() {
        return MethodSpec.methodBuilder("createSchema")
                .addModifiers(Modifier.PUBLIC)
                .returns(Schema.class)
                .addStatement("return this.schema")
                .build();
    }

    private static CodeBlock createSchema(TypeManifest type, PrefabContext context) {
        return switch (type) {
            case TypeManifest t when isLogicalType(t) -> createLogicalSchema(type);
            case TypeManifest t when t.isStandardType() -> createPrimitiveSchema(type, context);
            case TypeManifest t when t.isEnum() -> createEnumSchema(type);
            default -> createRecordSchema(type, context);
        };
    }

    private static CodeBlock createLogicalSchema(TypeManifest type) {
        if (type.is(Instant.class)) {
            return CodeBlock.of("$T.createLogicalSchema($T.LONG, $T.timestampMillis())",
                    SchemaSupport.class,
                    Schema.Type.class,
                    LogicalTypes.class);
        } else if (type.is(LocalDate.class)) {
            return CodeBlock.of("$T.createLogicalSchema($T.INT, $T.date())",
                    SchemaSupport.class,
                    Schema.Type.class,
                    LogicalTypes.class);
        } else if (type.is(Duration.class)) {
            return CodeBlock.of("$T.createLogicalSchema($T.LONG, $T.DURATION_MILLIS)",
                    SchemaSupport.class,
                    Schema.Type.class,
                    SchemaSupport.class);
        } else if (type.is(Reference.class)) {
            return CodeBlock.of("$T.createLogicalSchema($T.STRING, $T.REFERENCE)",
                    SchemaSupport.class,
                    Schema.Type.class,
                    SchemaSupport.class);
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    private static CodeBlock createEnumSchema(TypeManifest type) {
        return CodeBlock.of("$T.createEnum($S, null, $S, $T.of($L))",
                Schema.class,
                type.simpleName().replace('.', '_'),
                type.packageName(),
                List.class,
                type.enumValues().stream()
                        .map(value -> CodeBlock.of("$S", value))
                        .collect(CodeBlock.joining(", ")));
    }

    private static CodeBlock createPrimitiveSchema(TypeManifest type, PrefabContext context) {
        Schema.Type schemaType;
        var primitiveType = type.asClass();
        if (primitiveType == String.class) {
            schemaType = Schema.Type.STRING;
        } else if (primitiveType == int.class || primitiveType == Integer.class) {
            schemaType = Schema.Type.INT;
        } else if (primitiveType == long.class || primitiveType == Long.class) {
            schemaType = Schema.Type.LONG;
        } else if (primitiveType == double.class || primitiveType == Double.class) {
            schemaType = Schema.Type.DOUBLE;
        } else if (primitiveType == float.class || primitiveType == Float.class) {
            schemaType = Schema.Type.FLOAT;
        } else if (primitiveType == boolean.class || primitiveType == Boolean.class) {
            schemaType = Schema.Type.BOOLEAN;
        } else {
            context.logError("Unsupported standard type: %s".formatted(type.asClass().getName()), type.asElement());
            schemaType = Schema.Type.NULL;
        }
        return CodeBlock.of("$T.create($T.$L)", Schema.class, Schema.Type.class, schemaType);
    }

    private static CodeBlock createRecordSchema(TypeManifest type, PrefabContext context) {
        if (type.isSealed()) {
            return CodeBlock.of("""
                            $T.createUnion($T.of(
                                $L
                            ))""",
                    Schema.class,
                    List.class,
                    type.permittedSubtypes().stream()
                            .map(subtype -> {
                                var schemaFactoryName = uncapitalize("%sSchemaFactory".formatted(subtype.simpleName().replace(".", "")));
                                return CodeBlock.of("$L.createSchema()", schemaFactoryName);
                            })
                            .collect(CodeBlock.joining(",\n    ")));
        } else {
            return CodeBlock.of("""
                            $T.createRecord($S, null, $S, false, $T.of(
                                    $L
                                ))""",
                    Schema.class,
                    type.simpleName().replace('.', '_'),
                    type.packageName(),
                    List.class,
                    type.fields().stream()
                            .map(field -> createField(field, context))
                            .collect(CodeBlock.joining(",\n        ")));
        }
    }

    private static CodeBlock createField(VariableManifest field, PrefabContext context) {
        var schema = maybeArray(field, context);
        if (field.hasAnnotation(Nullable.class)) {
            return CodeBlock.of("new $T($S, $L, null)",
                    Schema.Field.class,
                    field.name(),
                    CodeBlock.of("$T.createNullableSchema($L)", SchemaSupport.class, schema));
        } else {
            return CodeBlock.of("new $T($S, $L)",
                    Schema.Field.class,
                    field.name(),
                    schema);
        }
    }

    private static CodeBlock maybeArray(VariableManifest field, PrefabContext context) {
        return field.type().is(List.class)
                ? CodeBlock.of("$T.createArray($L)", Schema.class,
                maybeNested(field.type().parameters().getFirst(), context))
                : maybeNested(field.type(), context);
    }

    private static CodeBlock maybeNested(TypeManifest type, PrefabContext context) {
        return !isNestedRecord(type)
                ? createSchema(type, context)
                : CodeBlock.of("$L.createSchema()", uncapitalize("%sSchemaFactory".formatted(type.simpleName().replace(".", ""))));
    }
}
