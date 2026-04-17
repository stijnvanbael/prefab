package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.avro.SchemaSupport;
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
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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
    private final PrefabContext context;

    EventSchemaFactoryWriter(PrefabContext context) {
        this.context = context;
    }

    void writeSchemaFactory(TypeManifest event) {
        // @Avsc contract interfaces have no fields to build a schema from — generate a delegating
        // factory that builds a union of the concrete records' schemas instead.
        if (event.asElement() != null && event.asElement().getAnnotation(Avsc.class) != null) {
            writeAvscInterfaceSchemaFactory(event);
            return;
        }

        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.avro");

        var name = "%sSchemaFactory".formatted(event.simpleName().replace(".", ""));
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build());
        type.addMethod(constructor(event))
                .addMethod(createSchemaMethod());

        fileWriter.writeFile(event.packageName(), name, type.build());

    }

    /**
     * Generates a schema factory for an {@code @Avsc}-annotated contract interface.
     * <p>
     * When there is a single concrete record the factory simply delegates to that record's schema
     * factory. When multiple records exist the factory builds an Avro union schema.
     */
    private void writeAvscInterfaceSchemaFactory(TypeManifest contractInterface) {
        var implementations = context.eventElements()
                .filter(e -> e.getKind() == ElementKind.RECORD)
                .filter(e -> e.getInterfaces().stream()
                        .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                        .anyMatch(iface -> iface.equals(contractInterface.asElement())))
                .map(e -> TypeManifest.of(e.asType(), context.processingEnvironment()))
                .toList();

        // Skip round 1: concrete records are compiled in round 2 and not yet available.
        if (implementations.isEmpty()) {
            return;
        }

        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.avro");
        var name = "%sSchemaFactory".formatted(contractInterface.simpleName().replace(".", ""));

        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build());

        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        implementations.forEach(impl -> {
            var factoryName = uncapitalize("%sSchemaFactory".formatted(impl.simpleName().replace(".", "")));
            var factoryType = ClassName.get(impl.packageName() + ".infrastructure.avro", capitalize(factoryName));
            type.addField(FieldSpec.builder(factoryType, factoryName, Modifier.PRIVATE, Modifier.FINAL).build());
            constructor.addParameter(factoryType, factoryName)
                    .addStatement("this.$L = $L", factoryName, factoryName);
        });

        CodeBlock schemaInit;
        if (implementations.size() == 1) {
            var factoryName = uncapitalize("%sSchemaFactory"
                    .formatted(implementations.getFirst().simpleName().replace(".", "")));
            schemaInit = CodeBlock.of("this.schema = $L.createSchema()", factoryName);
        } else {
            schemaInit = CodeBlock.of("this.schema = $T.createUnion($T.of(\n    $L\n))",
                    Schema.class, List.class,
                    implementations.stream()
                            .map(impl -> CodeBlock.of("$L.createSchema()",
                                    uncapitalize("%sSchemaFactory".formatted(impl.simpleName().replace(".", "")))))
                            .collect(CodeBlock.joining(",\n    ")));
        }
        constructor.addStatement(schemaInit);
        type.addMethod(constructor.build())
                .addMethod(createSchemaMethod());

        fileWriter.writeFile(contractInterface.packageName(), name, type.build());
    }

    private MethodSpec constructor(TypeManifest event) {
        var constructor = getBuilder();
        nestedTypes(List.of(event)).forEach(nestedType -> addSchemaFactory(nestedType, constructor));
        sealedSubtypes(List.of(event)).forEach(subtype -> addSchemaFactory(subtype, constructor));
        return constructor
                .addStatement("this.schema = $L", createSchema(event))
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

    private CodeBlock createSchema(TypeManifest type) {
        return switch (type) {
            case TypeManifest t when isLogicalType(t) -> createLogicalSchema(type);
            case TypeManifest t when t.isStandardType() -> createPrimitiveSchema(type);
            case TypeManifest t when t.isEnum() -> createEnumSchema(type);
            case TypeManifest t when t.isCustomType() -> createCustomTypeSchema(type);
            default -> createRecordSchema(type);
        };
    }

    private CodeBlock createCustomTypeSchema(TypeManifest type) {
        return context.plugins().stream()
                .map(plugin -> plugin.avroSchemaOf(type))
                .filter(Optional::isPresent)
                .findFirst()
                .flatMap(opt -> opt)
                .orElseGet(() -> {
                    context.logError(
                            ("@CustomType '%s' has no Avro schema: no PrefabPlugin provides an avroSchemaOf() " +
                            "implementation. Implement PrefabPlugin.avroSchemaOf() to support Avro serialization " +
                            "for this type.").formatted(type),
                            type.asElement());
                    return CodeBlock.of("$T.create($T.NULL)", Schema.class, Schema.Type.class);
                });
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
        } else if (type.isSingleValueType()) {
            return CodeBlock.of("$T.create($T.STRING)",
                    Schema.class,
                    Schema.Type.class);
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

    private CodeBlock createPrimitiveSchema(TypeManifest type) {
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

    private CodeBlock createRecordSchema(TypeManifest type) {
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
                            .filter(field -> {
                                if (field.type().isCustomType() && context.plugins().stream()
                                        .noneMatch(p -> p.avroSchemaOf(field.type()).isPresent())) {
                                    context.processingEnvironment().getMessager().printMessage(
                                            javax.tools.Diagnostic.Kind.WARNING,
                                            ("Field '%s' of @CustomType '%s' is omitted from the Avro schema: no " +
                                            "PrefabPlugin provides an avroSchemaOf() implementation. Implement " +
                                            "PrefabPlugin.avroSchemaOf() to include this field in the Avro schema.")
                                                    .formatted(field.name(), field.type()),
                                            field.element());
                                    return false;
                                }
                                return true;
                            })
                            .map(this::createField)
                            .collect(CodeBlock.joining(",\n        ")));
        }
    }

    private CodeBlock createField(VariableManifest field) {
        var schema = maybeArray(field);
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

    private CodeBlock maybeArray(VariableManifest field) {
        return field.type().is(List.class)
                ? CodeBlock.of("$T.createArray($L)", Schema.class,
                maybeNested(field.type().parameters().getFirst()))
                : maybeNested(field.type());
    }

    private CodeBlock maybeNested(TypeManifest type) {
        return !isNestedRecord(type)
                ? createSchema(type)
                : CodeBlock.of("$L.createSchema()", uncapitalize("%sSchemaFactory".formatted(type.simpleName().replace(".", ""))));
    }
}
