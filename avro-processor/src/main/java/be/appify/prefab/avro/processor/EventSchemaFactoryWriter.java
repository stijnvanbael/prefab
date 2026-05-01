package be.appify.prefab.avro.processor;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import com.palantir.javapoet.*;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import static be.appify.prefab.avro.processor.AvroPlugin.isLogicalType;
import static be.appify.prefab.avro.processor.AvroPlugin.isNestedRecord;
import static be.appify.prefab.avro.processor.AvroPlugin.nestedTypes;
import static be.appify.prefab.avro.processor.AvroPlugin.sealedSubtypes;
import static be.appify.prefab.avro.processor.AvroSupport.componentAnnotation;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class EventSchemaFactoryWriter {

    private static final String AVRO_PACKAGE_SUFFIX = "infrastructure.avro";

    private final PrefabContext context;

    EventSchemaFactoryWriter(PrefabContext context) {
        this.context = context;
    }

    void writeSchemaFactory(TypeManifest event) {
        if (isAvscContractInterface(event)) {
            writeAvscInterfaceSchemaFactory(event);
            return;
        }

        if (isAvscGeneratedRecord(event)) {
            findAvscPath(event).ifPresent(path -> writeAvscRecordSchemaFactory(event, path));
            return;
        }

        var name = schemaFactorySimpleName(event);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(event, name))
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor(event))
                .addMethod(createSchemaMethod());

        newFileWriter(event).writeFile(event.packageName(), name, type.build());
    }

    private void writeAvscRecordSchemaFactory(TypeManifest event, String avscPath) {
        var name = schemaFactorySimpleName(event);
        var schemaFactoryClass = ClassName.get(event.packageName() + "." + AVRO_PACKAGE_SUFFIX, name);
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("try (var stream = $T.class.getClassLoader().getResourceAsStream($S))",
                        schemaFactoryClass, avscPath)
                .beginControlFlow("if (stream == null)")
                .addStatement("throw new $T($S)", IllegalStateException.class,
                        "AVSC file not found on classpath: " + avscPath)
                .endControlFlow()
                .addStatement("this.schema = new $T().parse(stream)", Schema.Parser.class)
                .nextControlFlow("catch ($T e)", IOException.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .build();

        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(event, name))
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor)
                .addMethod(createSchemaMethod());

        newFileWriter(event).writeFile(event.packageName(), name, type.build());
    }

    private boolean isAvscGeneratedRecord(TypeManifest event) {
        return event.asElement() != null
                && EventPlatformPluginSupport.isAvscGeneratedRecord(event.asElement());
    }

    private Optional<String> findAvscPath(TypeManifest event) {
        var typeElement = (TypeElement) event.asElement();
        return typeElement.getInterfaces().stream()
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Avsc.class) != null)
                .flatMap(iface -> Arrays.stream(iface.getAnnotation(Avsc.class).value()))
                .filter(path -> matchesRecordName(path, event.simpleName()))
                .findFirst();
    }

    private boolean matchesRecordName(String avscPath, String recordSimpleName) {
        try (var stream = openResource(avscPath)) {
            if (stream == null) return false;
            return new Schema.Parser().parse(stream).getName().equals(recordSimpleName);
        } catch (IOException e) {
            return false;
        }
    }

    private InputStream openResource(String path) throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream != null) return stream;
        var file = Path.of("src/main/resources", path);
        if (Files.exists(file)) return Files.newInputStream(file);
        return null;
    }

    /**
     * Generates a schema factory for an {@code @Avsc}-annotated contract interface.
     * <p>
     * When there is a single concrete record the factory simply delegates to that record's schema
     * factory. When multiple records exist the factory builds an Avro union schema.
     */
    private void writeAvscInterfaceSchemaFactory(TypeManifest contractInterface) {
        var implementations = resolveImplementations(contractInterface);

        // Skip round 1: concrete records are compiled in round 2 and not yet available.
        if (implementations.isEmpty()) {
            return;
        }

        var name = schemaFactorySimpleName(contractInterface);
        var constructor = buildImplementationConstructor(implementations);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(contractInterface, name))
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor)
                .addMethod(createSchemaMethod());

        newFileWriter(contractInterface).writeFile(contractInterface.packageName(), name, type.build());
    }

    private List<TypeManifest> resolveImplementations(TypeManifest contractInterface) {
        return context.eventElements()
                .filter(e -> e.getKind() == ElementKind.RECORD)
                .filter(e -> implementsInterface(e, contractInterface))
                .map(e -> TypeManifest.of(e.asType(), context.processingEnvironment()))
                .toList();
    }

    private boolean implementsInterface(TypeElement element, TypeManifest contractInterface) {
        return element.getInterfaces().stream()
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .anyMatch(iface -> iface.equals(contractInterface.asElement()));
    }

    private MethodSpec buildImplementationConstructor(List<TypeManifest> implementations) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        implementations.forEach(impl -> addSchemaFactoryField(impl, constructor));
        constructor.addStatement(buildSchemaInitializer(implementations));
        return constructor.build();
    }

    private void addSchemaFactoryField(TypeManifest impl, MethodSpec.Builder constructor) {
        var fieldName = schemaFactoryFieldName(impl);
        var fieldType = schemaFactoryClassName(impl);
        constructor.addParameter(fieldType, fieldName)
                .addStatement("this.$L = $L", fieldName, fieldName);
    }

    private CodeBlock buildSchemaInitializer(List<TypeManifest> implementations) {
        if (implementations.size() == 1) {
            return CodeBlock.of("this.schema = $L.createSchema()", schemaFactoryFieldName(implementations.getFirst()));
        }
        return CodeBlock.of("this.schema = $T.createUnion($T.of(\n    $L\n))",
                Schema.class, List.class,
                implementations.stream()
                        .map(impl -> CodeBlock.of("$L.createSchema()", schemaFactoryFieldName(impl)))
                        .collect(CodeBlock.joining(",\n    ")));
    }

    private MethodSpec constructor(TypeManifest event) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        nestedTypes(List.of(event)).forEach(nestedType -> addSchemaFactory(nestedType, constructor));
        sealedSubtypes(List.of(event)).forEach(subtype -> addSchemaFactory(subtype, constructor));
        return constructor
                .addStatement("this.schema = $L", createSchema(event))
                .build();
    }

    private static void addSchemaFactory(TypeManifest nestedType, MethodSpec.Builder constructor) {
        constructor.addParameter(schemaFactoryClassName(nestedType), schemaFactoryFieldName(nestedType));
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
                    SchemaSupport.class, Schema.Type.class, LogicalTypes.class);
        } else if (type.is(LocalDate.class)) {
            return CodeBlock.of("$T.createLogicalSchema($T.INT, $T.date())",
                    SchemaSupport.class, Schema.Type.class, LogicalTypes.class);
        } else if (type.is(Duration.class)) {
            return CodeBlock.of("$T.createLogicalSchema($T.LONG, $T.DURATION_MILLIS)",
                    SchemaSupport.class, Schema.Type.class, SchemaSupport.class);
        } else if (type.isSingleValueType()) {
            return CodeBlock.of("$T.create($T.STRING)", Schema.class, Schema.Type.class);
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
        var schemaType = primitiveSchemaType(type);
        return CodeBlock.of("$T.create($T.$L)", Schema.class, Schema.Type.class, schemaType);
    }

    private Schema.Type primitiveSchemaType(TypeManifest type) {
        var primitiveType = type.asClass();
        if (primitiveType == String.class) return Schema.Type.STRING;
        if (primitiveType == int.class || primitiveType == Integer.class) return Schema.Type.INT;
        if (primitiveType == long.class || primitiveType == Long.class) return Schema.Type.LONG;
        if (primitiveType == double.class || primitiveType == Double.class) return Schema.Type.DOUBLE;
        if (primitiveType == float.class || primitiveType == Float.class) return Schema.Type.FLOAT;
        if (primitiveType == boolean.class || primitiveType == Boolean.class) return Schema.Type.BOOLEAN;

        context.logError("Unsupported standard type: %s".formatted(type.asClass().getName()), type.asElement());
        return Schema.Type.NULL;
    }

    private CodeBlock createRecordSchema(TypeManifest type) {
        return type.isSealed()
                ? createUnionSchema(type)
                : createFlatRecordSchema(type);
    }

    private CodeBlock createUnionSchema(TypeManifest type) {
        return CodeBlock.of("""
                        $T.createUnion($T.of(
                            $L
                        ))""",
                Schema.class,
                List.class,
                type.permittedSubtypes().stream()
                        .map(subtype -> CodeBlock.of("$L.createSchema()", schemaFactoryFieldName(subtype)))
                        .collect(CodeBlock.joining(",\n    ")));
    }

    private CodeBlock createFlatRecordSchema(TypeManifest type) {
        return CodeBlock.of("""
                        $T.createRecord($S, $S, $S, false, $T.of(
                                $L
                            ))""",
                Schema.class,
                type.simpleName().replace('.', '_'),
                type.doc().orElse(null),
                type.packageName(),
                List.class,
                type.fields().stream()
                        .filter(this::hasAvroSchema)
                        .map(this::createField)
                        .collect(CodeBlock.joining(",\n        ")));
    }

    private boolean hasAvroSchema(VariableManifest field) {
        if (!field.type().isCustomType()) {
            return true;
        }
        boolean supported = context.plugins().stream().anyMatch(p -> p.avroSchemaOf(field.type()).isPresent());
        if (!supported) {
            context.processingEnvironment().getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    ("Field '%s' of @CustomType '%s' is omitted from the Avro schema: no " +
                    "PrefabPlugin provides an avroSchemaOf() implementation. Implement " +
                    "PrefabPlugin.avroSchemaOf() to include this field in the Avro schema.")
                            .formatted(field.name(), field.type()),
                    field.element());
        }
        return supported;
    }

    private CodeBlock createField(VariableManifest field) {
        var schema = maybeArray(field);
        var doc = field.getAnnotation(Doc.class).map(d -> d.value().value()).orElse(null);
        var fieldBlock = buildFieldBlock(field, schema, doc);
        return wrapWithSampleIfPresent(field, fieldBlock);
    }

    private static CodeBlock buildFieldBlock(VariableManifest field, CodeBlock schema, @Nullable String doc) {
        if (field.hasAnnotation(Nullable.class)) {
            var nullableSchema = CodeBlock.of("$T.createNullableSchema($L)", SchemaSupport.class, schema);
            return CodeBlock.of("new $T($S, $L, $S, $T.NULL_DEFAULT_VALUE)",
                    Schema.Field.class, field.name(), nullableSchema, doc, Schema.Field.class);
        }
        return doc != null
                ? CodeBlock.of("new $T($S, $L, $S, null)", Schema.Field.class, field.name(), schema, doc)
                : CodeBlock.of("new $T($S, $L)", Schema.Field.class, field.name(), schema);
    }

    private static CodeBlock wrapWithSampleIfPresent(VariableManifest field, CodeBlock fieldBlock) {
        return field.getAnnotation(Example.class)
                .map(example -> CodeBlock.of("$T.withSample($L, $S)",
                        SchemaSupport.class,
                        fieldBlock,
                        example.value().value()))
                .orElse(fieldBlock);
    }

    private CodeBlock maybeArray(VariableManifest field) {
        return field.type().is(List.class)
                ? CodeBlock.of("$T.createArray($L)", Schema.class, maybeNested(field.type().parameters().getFirst()))
                : maybeNested(field.type());
    }

    private CodeBlock maybeNested(TypeManifest type) {
        return isNestedRecord(type)
                ? CodeBlock.of("$L.createSchema()", schemaFactoryFieldName(type))
                : createSchema(type);
    }

    private JavaFileWriter newFileWriter(TypeManifest event) {
        return new JavaFileWriter(context.processingEnvironment(), AVRO_PACKAGE_SUFFIX);
    }

    private boolean isAvscContractInterface(TypeManifest event) {
        return event.asElement() != null && event.asElement().getAnnotation(Avsc.class) != null;
    }

    private static String schemaFactorySimpleName(TypeManifest type) {
        return "%sSchemaFactory".formatted(type.simpleName().replace(".", ""));
    }

    private static String schemaFactoryFieldName(TypeManifest type) {
        return uncapitalize(schemaFactorySimpleName(type));
    }

    private static ClassName schemaFactoryClassName(TypeManifest type) {
        return ClassName.get(type.packageName() + "." + AVRO_PACKAGE_SUFFIX, capitalize(schemaFactoryFieldName(type)));
    }
}
