package be.appify.prefab.avro.processor;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Example;
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
import javax.lang.model.element.Element;
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
        avscPath.ifPresent(path -> validateGeneratedSchemaAgainstAvsc(event, path));

        var name = schemaFactorySimpleName(event);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(event, name))
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor(event, preserveSingleValueRecords))
                .addMethod(createSchemaMethod());

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
        return findAvscPath(type)
                .flatMap(path -> namedTypeFromAvsc(path, type.simpleName()))
                .map(Schema::getNamespace)
                .filter(namespace -> namespace != null && !namespace.isBlank())
                .orElse(type.packageName());
    }

    private Optional<Schema> namedTypeFromAvsc(String avscPath, String simpleName) {
        try (var stream = openResource(avscPath)) {
            if (stream == null) {
                return Optional.empty();
            }
            var parsedSchema = new Schema.Parser().parse(stream);
            return Optional.ofNullable(SchemaSupport.namedTypeOf(parsedSchema, simpleName));
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
            if (stream == null) return false;
            return containsNamedType(new Schema.Parser().parse(stream), recordSimpleName);
        } catch (IOException e) {
            context.processingEnvironment().getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    "Could not read or parse AVSC file '%s' while resolving schema for '%s': %s"
                            .formatted(avscPath, recordSimpleName, e.getMessage()));
            return false;
        }
    }

    private void validateGeneratedSchemaAgainstAvsc(TypeManifest event, String avscPath) {
        try (var stream = openResource(avscPath)) {
            if (stream == null) {
                context.logError("AVSC file not found on classpath: %s".formatted(avscPath), event.asElement());
                return;
            }

            var parsedSchema = new Schema.Parser().parse(stream);
            var expectedSchema = SchemaSupport.namedTypeOf(parsedSchema, event.simpleName());
            var generatedSchema = schemaModelOf(event, true);
            var expectedReadsGenerated = SchemaCompatibility.checkReaderWriterCompatibility(expectedSchema, generatedSchema);
            var generatedReadsExpected = SchemaCompatibility.checkReaderWriterCompatibility(generatedSchema, expectedSchema);
            if (expectedReadsGenerated.getType() != SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE
                    || generatedReadsExpected.getType() != SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE) {
                context.logError(
                        ("Generated schema factory for '%s' is not compatible with AVSC '%s'. "
                        + "This likely indicates a bug in Prefab schema generation; please report it with this error. "
                        + "expected->generated: %s; generated->expected: %s. "
                        + "expectedSchema=%s; generatedSchema=%s")
                                .formatted(
                                        event.simpleName(),
                                        avscPath,
                                        compatibilityDetails(expectedReadsGenerated),
                                        compatibilityDetails(generatedReadsExpected),
                                        compactSchema(expectedSchema),
                                        compactSchema(generatedSchema)),
                        event.asElement());
            }
        } catch (IOException e) {
            context.logError(
                    "Could not read AVSC file '%s' for '%s': %s"
                            .formatted(avscPath, event.simpleName(), e.getMessage()),
                    event.asElement());
        } catch (RuntimeException e) {
            context.logError(
                    "Could not validate AVSC '%s' for '%s': %s"
                            .formatted(avscPath, event.simpleName(), e.getMessage()),
                    event.asElement());
        }
    }

    private static String compatibilityDescription(SchemaCompatibility.SchemaPairCompatibility compatibility) {
        var description = compatibility.getDescription();
        if (description == null || description.isBlank()) {
            return compatibility.toString();
        }
        return description;
    }

    private static String compatibilityDetails(SchemaCompatibility.SchemaPairCompatibility compatibility) {
        var incompatibilities = compatibility.getResult().getIncompatibilities();
        if (incompatibilities == null || incompatibilities.isEmpty()) {
            return sanitizeDiagnosticText(compatibilityDescription(compatibility));
        }

        var shown = incompatibilities.stream()
                .limit(8)
                .map(EventSchemaFactoryWriter::formatIncompatibility)
                .collect(java.util.stream.Collectors.joining(" | "));
        var remaining = incompatibilities.size() - 8;
        if (remaining <= 0) {
            return shown;
        }
        return shown + " | ... (" + remaining + " more incompatibilities)";
    }

    private static String formatIncompatibility(SchemaCompatibility.Incompatibility incompatibility) {
        var location = incompatibility.getLocation();
        var locationLabel = location.isBlank() ? "<root>" : location;
        var message = sanitizeDiagnosticText(incompatibility.getMessage());
        return "%s at %s: %s".formatted(incompatibility.getType(), locationLabel, message);
    }

    private static String sanitizeDiagnosticText(String text) {
        if (text == null) {
            return "<no details>";
        }
        return text.replaceAll("\\R+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String compactSchema(Schema schema) {
        var schemaText = schema.toString();
        var maxLength = 700;
        if (schemaText.length() <= maxLength) {
            return schemaText;
        }
        return schemaText.substring(0, maxLength) + "...<truncated>";
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

    private MethodSpec constructor(TypeManifest event, boolean preserveSingleValueRecords) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        nestedTypes(List.of(event)).forEach(nestedType -> addSchemaFactory(nestedType, constructor));
        sealedSubtypes(List.of(event)).forEach(subtype -> addSchemaFactory(subtype, constructor));
        return constructor
                .addStatement("this.schema = $L", createSchema(event, preserveSingleValueRecords))
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
            case TypeManifest t when isSingleValueScalarType(t) && !preserveSingleValueRecords ->
                    CodeBlock.of("$T.create($T.STRING)", Schema.class, Schema.Type.class);
            case TypeManifest t when t.isStandardType() -> createPrimitiveSchema(type);
            case TypeManifest t when t.isEnum() -> createEnumSchema(type);
            case TypeManifest t when t.isCustomType() -> createCustomTypeSchema(type);
            default -> createRecordSchema(type, preserveSingleValueRecords);
        };
    }

    private static boolean isTemporalLogicalType(TypeManifest type) {
        return type.is(Instant.class) || type.is(LocalDate.class) || type.is(Duration.class);
    }

    private static boolean isSingleValueScalarType(TypeManifest type) {
        return type.isSingleValueType() && type.fields().getFirst().type().isStandardType();
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
                ? createUnionSchema(type, preserveSingleValueRecords)
                : createFlatRecordSchema(type, preserveSingleValueRecords);
    }

    private CodeBlock createUnionSchema(TypeManifest type, boolean preserveSingleValueRecords) {
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

    private Schema schemaModelOf(TypeManifest type, boolean preserveSingleValueRecords) {
        if (isTemporalLogicalType(type)) {
            return logicalSchemaModelOf(type);
        }
        if (isSingleValueScalarType(type) && !preserveSingleValueRecords) {
            return Schema.create(Schema.Type.STRING);
        }
        if (type.isStandardType()) {
            return primitiveSchemaModelOf(type);
        }
        if (type.isEnum()) {
            return enumSchemaModelOf(type);
        }
        if (type.isCustomType()) {
            context.logError(
                    ("@CustomType '%s' has no schema model for AVSC validation. " +
                    "Implementing compile-time validation for this type requires plugin-provided schema modeling.")
                            .formatted(type),
                    type.asElement());
            return Schema.create(Schema.Type.NULL);
        }
        return recordSchemaModelOf(type, preserveSingleValueRecords);
    }

    private Schema logicalSchemaModelOf(TypeManifest type) {
        if (type.is(Instant.class)) {
            return SchemaSupport.createLogicalSchema(Schema.Type.LONG, LogicalTypes.timestampMillis());
        }
        if (type.is(LocalDate.class)) {
            return SchemaSupport.createLogicalSchema(Schema.Type.INT, LogicalTypes.date());
        }
        if (type.is(Duration.class)) {
            return SchemaSupport.createLogicalSchema(Schema.Type.LONG, SchemaSupport.DURATION_MILLIS);
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    private Schema enumSchemaModelOf(TypeManifest type) {
        return Schema.createEnum(
                type.simpleName().replace('.', '_'),
                null,
                avroNamespaceOf(type),
                type.enumValues());
    }

    private Schema primitiveSchemaModelOf(TypeManifest type) {
        return Schema.create(primitiveSchemaType(type));
    }

    private Schema recordSchemaModelOf(TypeManifest type, boolean preserveSingleValueRecords) {
        if (type.isSealed()) {
            return Schema.createUnion(type.permittedSubtypes().stream()
                    .map(subtype -> schemaModelOf(subtype, preserveSingleValueRecords))
                    .toList());
        }

        return Schema.createRecord(
                type.simpleName().replace('.', '_'),
                type.doc().orElse(null),
                avroNamespaceOf(type),
                false,
                type.fields().stream()
                        .filter(this::hasAvroSchema)
                        .map(field -> fieldSchemaModelOf(field, preserveSingleValueRecords))
                        .toList());
    }

    private Schema.Field fieldSchemaModelOf(VariableManifest field, boolean preserveSingleValueRecords) {
        var schema = maybeArraySchemaModelOf(field, preserveSingleValueRecords);
        var doc = field.getAnnotation(Doc.class).map(d -> d.value().value()).orElse(null);

        Schema.Field avroField;
        if (field.hasAnnotation(Nullable.class)) {
            avroField = new Schema.Field(
                    field.name(),
                    SchemaSupport.createNullableSchema(schema),
                    doc,
                    Schema.Field.NULL_DEFAULT_VALUE);
        } else if (doc != null) {
            avroField = new Schema.Field(field.name(), schema, doc, null);
        } else {
            avroField = new Schema.Field(field.name(), schema);
        }

        field.getAnnotation(Example.class)
                .ifPresent(example -> SchemaSupport.withSample(avroField, example.value().value()));

        return avroField;
    }

    private Schema maybeArraySchemaModelOf(VariableManifest field, boolean preserveSingleValueRecords) {
        if (field.type().is(List.class)) {
            return Schema.createArray(schemaModelOf(field.type().parameters().getFirst(), preserveSingleValueRecords));
        }
        return schemaModelOf(field.type(), preserveSingleValueRecords);
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
