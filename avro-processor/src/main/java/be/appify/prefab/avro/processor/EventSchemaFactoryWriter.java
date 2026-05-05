package be.appify.prefab.avro.processor;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.Namespace;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.*;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;

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

        var avscPath = findAvscPath(event);
        var preserveSingleValueRecords = avscPath.isPresent();

        var name = schemaFactorySimpleName(event);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(event, name))
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor(event, preserveSingleValueRecords, avscPath.orElse(null)))
                .addMethod(createSchemaMethod());
        avscPath.ifPresent(path -> type.addMethods(runtimeAvscValidationMethods(event, path)));

        newFileWriter().writeFile(event.packageName(), name, type.build());
    }

    private Optional<String> findAvscPath(TypeManifest event) {
        var recordSimpleName = event.simpleName();

        if (event.asElement() instanceof TypeElement typeElement) {
            var directMatch = avscPathsFromImplementedInterfaces(typeElement)
                    .filter(path -> matchesRecordName(path, recordSimpleName))
                    .findFirst();
            if (directMatch.isPresent()) {
                return directMatch;
            }
        }

        return avscAnnotatedInterfaces()
                .flatMap(e -> Arrays.stream(Objects.requireNonNull(e.getAnnotation(Avsc.class)).value()))
                .filter(path -> matchesRecordName(path, recordSimpleName))
                .findFirst();
    }

    private String avroNamespaceOf(TypeManifest type) {
        return type.annotationsOfType(Namespace.class).stream()
                .map(Namespace::value)
                .filter(namespace -> !namespace.isBlank())
                .findFirst()
                .or(() -> findAvscPath(type)
                .flatMap(path -> namedTypeFromAvsc(path, type.simpleName()))
                .map(Schema::getNamespace)
                .filter(namespace -> !namespace.isBlank()))
                .orElse(type.packageName());
    }

    private Optional<Schema> namedTypeFromAvsc(String avscPath, String simpleName) {
        try (var stream = openResource(avscPath)) {
            if (stream == null) {
                return Optional.empty();
            }
            var parsedSchema = new Schema.Parser().parse(stream);
            return avroTypeNames(simpleName)
                    .filter(typeName -> containsNamedType(parsedSchema, typeName))
                    .findFirst()
                    .map(typeName -> SchemaSupport.namedTypeOf(parsedSchema, typeName));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private java.util.stream.Stream<TypeElement> avscAnnotatedInterfaces() {
        return context.roundEnvironment().getElementsAnnotatedWith(Avsc.class).stream()
                .filter(element -> element.getKind().isInterface())
                .map(TypeElement.class::cast)
                .filter(type -> type.getAnnotation(Avsc.class) != null);
    }

    private java.util.stream.Stream<String> avscPathsFromImplementedInterfaces(TypeElement typeElement) {
        return typeElement.getInterfaces().stream()
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Avsc.class) != null)
                .flatMap(iface -> Arrays.stream(Objects.requireNonNull(iface.getAnnotation(Avsc.class)).value()));
    }

    private boolean matchesRecordName(String avscPath, String recordSimpleName) {
        try (var stream = openResource(avscPath)) {
            if (stream == null) {
                return false;
            }
            var parsedSchema = new Schema.Parser().parse(stream);
            return avroTypeNames(recordSimpleName).anyMatch(typeName -> containsNamedType(parsedSchema, typeName));
        } catch (IOException e) {
            context.processingEnvironment().getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    "Could not read or parse AVSC file '%s' while resolving schema for '%s': %s"
                            .formatted(avscPath, recordSimpleName, e.getMessage()));
            return false;
        }
    }

    private Stream<String> avroTypeNames(String javaSimpleName) {
        var avroSimpleName = toAvroSimpleName(javaSimpleName);
        if (javaSimpleName.equals(avroSimpleName)) {
            return Stream.of(javaSimpleName);
        }
        return Stream.of(javaSimpleName, avroSimpleName);
    }

    private String toAvroSimpleName(String javaSimpleName) {
        var lastDotIndex = javaSimpleName.lastIndexOf('.');
        return lastDotIndex >= 0 ? javaSimpleName.substring(lastDotIndex + 1) : javaSimpleName;
    }

    private boolean containsNamedType(Schema schema, String simpleName) {
        return containsNamedType(schema, simpleName, new HashSet<>());
    }

    private boolean containsNamedType(Schema schema, String simpleName, Set<String> visited) {
        if (schema == null) {
            return false;
        }

        switch (schema.getType()) {
            case RECORD -> {
                var fullName = schema.getFullName();
                if (fullName != null && !visited.add(fullName)) {
                    return false;
                }
                if (schema.getName().equals(simpleName)) {
                    return true;
                }
                return schema.getFields().stream().anyMatch(field -> containsNamedType(field.schema(), simpleName, visited));
            }
            case ENUM -> {
                return schema.getName().equals(simpleName);
            }
            case ARRAY -> {
                return containsNamedType(schema.getElementType(), simpleName, visited);
            }
            case UNION -> {
                return schema.getTypes().stream().anyMatch(type -> containsNamedType(type, simpleName, visited));
            }
            default -> {
                return false;
            }
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

        newFileWriter().writeFile(contractInterface.packageName(), name, type.build());
    }

    private List<TypeManifest> resolveImplementations(TypeManifest contractInterface) {
        return context.eventElementsFromCurrentCompilation()
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

    private MethodSpec constructor(TypeManifest event, boolean preserveSingleValueRecords, @Nullable String avscPath) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        nestedTypes(List.of(event)).forEach(nestedType -> addSchemaFactory(nestedType, constructor));
        sealedSubtypes(List.of(event)).forEach(subtype -> addSchemaFactory(subtype, constructor));
        constructor.addStatement("this.schema = $L", createSchema(event, preserveSingleValueRecords));
        if (avscPath != null) {
            constructor.addStatement("verifySchemaCompatibility(this.schema)");
        }
        return constructor.build();
    }

    private List<MethodSpec> runtimeAvscValidationMethods(TypeManifest event, String avscPath) {
        return List.of(
                runtimeCompatibilityVerifierMethod(event, avscPath),
                runtimeExpectedSchemaLoaderMethod(event, avscPath),
                runtimeCompactSchemaMethod());
    }

    private MethodSpec runtimeCompatibilityVerifierMethod(TypeManifest event, String avscPath) {
        return MethodSpec.methodBuilder("verifySchemaCompatibility")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(Schema.class, "generatedSchema")
                .addStatement("var expectedSchema = loadExpectedSchema()")
                .addStatement("var expectedReadsGenerated = $T.checkReaderWriterCompatibility(expectedSchema, generatedSchema)",
                        SchemaCompatibility.class)
                .addStatement("var generatedReadsExpected = $T.checkReaderWriterCompatibility(generatedSchema, expectedSchema)",
                        SchemaCompatibility.class)
                .beginControlFlow("if (expectedReadsGenerated.getType() != $T.SchemaCompatibilityType.COMPATIBLE"
                                + " || generatedReadsExpected.getType() != $T.SchemaCompatibilityType.COMPATIBLE)",
                        SchemaCompatibility.class,
                        SchemaCompatibility.class)
                .addStatement("throw new $T(\"Generated schema factory for '$L' is not compatible with AVSC '$L'. \""
                                + " + \"expected->generated: \" + expectedReadsGenerated.getDescription()"
                                + " + \"; generated->expected: \" + generatedReadsExpected.getDescription()"
                                + " + \". expectedSchema=\" + compactSchema(expectedSchema)"
                                + " + \"; generatedSchema=\" + compactSchema(generatedSchema))",
                        IllegalStateException.class,
                        event.simpleName(),
                        avscPath)
                .endControlFlow()
                .build();
    }

    private MethodSpec runtimeExpectedSchemaLoaderMethod(TypeManifest event, String avscPath) {
        var expectedSchemaName = toAvroSimpleName(event.simpleName());
        return MethodSpec.methodBuilder("loadExpectedSchema")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(Schema.class)
                .beginControlFlow("try (var stream = $T.class.getClassLoader().getResourceAsStream($S))",
                        schemaFactoryClassName(event),
                        avscPath)
                .beginControlFlow("if (stream == null)")
                .addStatement("throw new $T(\"AVSC file not found on classpath: $L\")",
                        IllegalStateException.class,
                        avscPath)
                .endControlFlow()
                .addStatement("var parsedSchema = new $T().parse(stream)", Schema.Parser.class)
                .addStatement("return $T.namedTypeOf(parsedSchema, $S)", SchemaSupport.class, expectedSchemaName)
                .nextControlFlow("catch ($T e)", IOException.class)
                .addStatement("throw new $T(\"Could not read AVSC '$L' for '$L': \" + e.getMessage(), e)",
                        IllegalStateException.class,
                        avscPath,
                        event.simpleName())
                .nextControlFlow("catch ($T e)", RuntimeException.class)
                .addStatement("throw new $T(\"Could not validate AVSC '$L' for '$L': \" + e.getMessage(), e)",
                        IllegalStateException.class,
                        avscPath,
                        event.simpleName())
                .endControlFlow()
                .build();
    }

    private MethodSpec runtimeCompactSchemaMethod() {
        return MethodSpec.methodBuilder("compactSchema")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(Schema.class, "schema")
                .addStatement("var schemaText = schema.toString()")
                .addStatement("var maxLength = 700")
                .beginControlFlow("if (schemaText.length() <= maxLength)")
                .addStatement("return schemaText")
                .endControlFlow()
                .addStatement("return schemaText.substring(0, maxLength) + $S", "...<truncated>")
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

    private CodeBlock createSchema(TypeManifest type, boolean preserveSingleValueRecords) {
        return switch (type) {
            case TypeManifest t when isTemporalLogicalType(t) -> createLogicalSchema(type);
            case TypeManifest t when t.isStandardType() -> createPrimitiveSchema(type);
            case TypeManifest t when t.isEnum() -> createEnumSchema(type);
            case TypeManifest t when t.isCustomType() -> createCustomTypeSchema(type);
            default -> createRecordSchema(type, preserveSingleValueRecords);
        };
    }

    private static boolean isTemporalLogicalType(TypeManifest type) {
        return type.is(Instant.class) || type.is(LocalDate.class) || type.is(Duration.class);
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
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    private CodeBlock createEnumSchema(TypeManifest type) {
        return CodeBlock.of("$T.createEnum($S, null, $S, $T.of($L))",
                Schema.class,
                type.simpleName().replace('.', '_'),
                avroNamespaceOf(type),
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

    private CodeBlock createRecordSchema(TypeManifest type, boolean preserveSingleValueRecords) {
        return type.isSealed()
                ? createUnionSchema(type)
                : createFlatRecordSchema(type, preserveSingleValueRecords);
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

    private CodeBlock createFlatRecordSchema(TypeManifest type, boolean preserveSingleValueRecords) {
        return CodeBlock.of("""
                        $T.createRecord($S, $S, $S, false, $T.of(
                                $L
                            ))""",
                Schema.class,
                type.simpleName().replace('.', '_'),
                type.doc().orElse(null),
                avroNamespaceOf(type),
                List.class,
                type.fields().stream()
                        .filter(this::hasAvroSchema)
                        .map(field -> createField(field, preserveSingleValueRecords))
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

    private CodeBlock createField(VariableManifest field, boolean preserveSingleValueRecords) {
        var schema = maybeArray(field, preserveSingleValueRecords);
        var doc = field.getAnnotation(Doc.class).map(d -> d.value().value()).orElse(null);
        var fieldBlock = buildFieldBlock(field, schema, doc);
        return wrapWithSampleIfPresent(field, fieldBlock);
    }

    private static CodeBlock buildFieldBlock(VariableManifest field, CodeBlock schema, @Nullable String doc) {
        if (field.nullable()) {
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

    private CodeBlock maybeArray(VariableManifest field, boolean preserveSingleValueRecords) {
        return field.type().is(List.class)
                ? CodeBlock.of("$T.createArray($L)", Schema.class,
                        maybeNested(field.type().parameters().getFirst(), preserveSingleValueRecords))
                : maybeNested(field.type(), preserveSingleValueRecords);
    }

    private CodeBlock maybeNested(TypeManifest type, boolean preserveSingleValueRecords) {
        return isNestedRecord(type)
                ? CodeBlock.of("$L.createSchema()", schemaFactoryFieldName(type))
                : createSchema(type, preserveSingleValueRecords);
    }


    private JavaFileWriter newFileWriter() {
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
