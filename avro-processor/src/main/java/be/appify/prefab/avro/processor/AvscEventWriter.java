package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.AvroSchema;
import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.BuilderWriter;
import be.appify.prefab.processor.FileOutput;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
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
import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;

import static com.palantir.javapoet.CodeBlock.joining;
import static java.util.Arrays.stream;

class AvscEventWriter {
    private static final String PROP_SAMPLE = "sample";
    private static final String PROP_LOGICAL_TYPE = "logicalType";
    private static final String LOGICAL_TYPE_TIMESTAMP_MILLIS = "timestamp-millis";
    private static final String LOGICAL_TYPE_DATE = "date";
    private static final String LOGICAL_TYPE_DURATION_MILLIS = "duration-millis";
    private static final String OPTION_SETTER_PREFIX = "prefab.builder.setterPrefix";
    private final ProcessingEnvironment processingEnvironment;
    private final FileOutput fileWriter;
    AvscEventWriter(PrefabContext context) {
        this.processingEnvironment = context.processingEnvironment();
        this.fileWriter = new OutputTargetFileOutput(context, "", OutputTarget.MAIN);
    }
    void writeAll(Schema schema, String[] topics, Event.Platform platform, String defaultPackage,
            ClassName contractInterface, List<AnnotationSpec> generateAnnotations) {
        var namedTypes = collectNamedTypes(schema);
        var pendingUnions = new ArrayList<UnionTypeGroup>();
        writeTopLevelRecord(schema, topics, platform, defaultPackage, contractInterface, generateAnnotations, fileWriter, pendingUnions);
        writeNestedTypes(schema, namedTypes, defaultPackage, fileWriter, pendingUnions);
        writeUnionTypes(pendingUnions, defaultPackage, fileWriter);
    }
    private void writeTopLevelRecord(Schema schema, String[] topics, Event.Platform platform,
            String defaultPackage, ClassName contractInterface, List<AnnotationSpec> generateAnnotations,
            FileOutput fileWriter, List<UnionTypeGroup> pendingUnions) {
        var topLevelSpec = buildTopLevelRecord(schema, topics, platform, defaultPackage, contractInterface, generateAnnotations, pendingUnions);
        if (topLevelSpec != null) {
            fileWriter.writeFile(defaultPackage, javaTypeName(schema), topLevelSpec);
        }
    }
    private void writeNestedTypes(Schema topLevelSchema, Map<String, Schema> namedTypes,
            String defaultPackage, FileOutput fileWriter, List<UnionTypeGroup> pendingUnions) {
        for (var entry : namedTypes.entrySet()) {
            var namedSchema = entry.getValue();
            if (namedSchema.equals(topLevelSchema)) continue;
            writeNestedType(namedSchema, defaultPackage, fileWriter, pendingUnions);
        }
    }
    private void writeNestedType(Schema schema, String defaultPackage,
            FileOutput fileWriter, List<UnionTypeGroup> pendingUnions) {
        if (schema.getType() == Schema.Type.RECORD) {
            var spec = buildNestedRecord(schema, defaultPackage, pendingUnions);
            if (spec != null) {
                fileWriter.writeFile(defaultPackage, javaTypeName(schema), spec);
            }
        } else if (schema.getType() == Schema.Type.ENUM) {
            fileWriter.writeFile(defaultPackage, javaTypeName(schema), buildEnum(schema));
        }
    }
    private void writeUnionTypes(List<UnionTypeGroup> pendingUnions, String defaultPackage, FileOutput fileWriter) {
        for (var group : pendingUnions) {
            fileWriter.writeFile(defaultPackage, group.interfaceName(), group.interfaceSpec());
            for (var branch : group.branches()) {
                fileWriter.writeFile(defaultPackage, branch.name(), branch.spec());
            }
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
    private TypeSpec buildTopLevelRecord(Schema schema, String[] topics, Event.Platform platform,
            String schemaPackage, ClassName contractInterface, List<AnnotationSpec> generateAnnotations,
            List<UnionTypeGroup> pendingUnions) {
        return buildFields(schema, schemaPackage, pendingUnions)
                .map(fields -> {
                    var typeName = javaTypeName(schema);
                    var recordType = ClassName.get(schemaPackage, typeName);
                    var builder = TypeSpec.recordBuilder(typeName)
                            .addModifiers(Modifier.PUBLIC)
                            .recordConstructor(MethodSpec.compactConstructorBuilder().addParameters(fields).build())
                            .addAnnotation(buildEventAnnotation(topics, platform))
                            .addSuperinterface(contractInterface);
                    generateAnnotations.forEach(builder::addAnnotation);
                    docOf(schema).ifPresent(doc -> builder.addAnnotation(docAnnotation(doc)));
                    avroSchemaAnnotation(schema).ifPresent(builder::addAnnotation);
                    new BuilderWriter(builderSetterPrefix()).enrichWithBuilder(builder, recordType,
                            strippedParams(fields), defaultsFor(schema, schemaPackage));
                    return builder.build();
                })
                .orElse(null);
    }
    private TypeSpec buildNestedRecord(Schema schema, String defaultPackage, List<UnionTypeGroup> pendingUnions) {
        return buildFields(schema, defaultPackage, pendingUnions)
                .map(fields -> {
                    var typeName = javaTypeName(schema);
                    var recordType = ClassName.get(defaultPackage, typeName);
                    var builder = TypeSpec.recordBuilder(typeName)
                            .addModifiers(Modifier.PUBLIC)
                            .recordConstructor(MethodSpec.compactConstructorBuilder().addParameters(fields).build());
                    docOf(schema).ifPresent(doc -> builder.addAnnotation(docAnnotation(doc)));
                    avroSchemaAnnotation(schema).ifPresent(builder::addAnnotation);
                    new BuilderWriter(builderSetterPrefix()).enrichWithBuilder(builder, recordType,
                            strippedParams(fields), defaultsFor(schema, defaultPackage));
                    return builder.build();
                })
                .orElse(null);
    }
    private Map<String, String> defaultsFor(Schema schema, String defaultPackage) {
        var result = new LinkedHashMap<String, String>();
        for (var field : schema.getFields()) {
            defaultInitialiserFor(field, defaultPackage).ifPresent(literal -> result.put(field.name(), literal));
        }
        return result;
    }
    private Optional<String> defaultInitialiserFor(Schema.Field field, String defaultPackage) {
        var defaultValue = field.defaultVal();
        if (defaultValue == null) {
            return Optional.empty();
        }
        if (defaultValue == JsonProperties.NULL_VALUE) {
            return Optional.of("null");
        }
        switch (defaultValue) {
            case String symbol -> {
                var resolvedSchema = resolvedNonNullSchema(field.schema());
                if (resolvedSchema != null && resolvedSchema.getType() == Schema.Type.ENUM) {
                    var enumTypeName = capitalize(resolvedSchema.getName());
                    return Optional.of(defaultPackage + "." + enumTypeName + "." + symbol);
                }
                return Optional.of("\"" + symbol.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
            }
            case Integer i -> {
                return Optional.of(i.toString());
            }
            case Long l -> {
                return Optional.of(l + "L");
            }
            case Double d -> {
                return Optional.of(d.toString());
            }
            case Float f -> {
                return Optional.of("(float) " + f);
            }
            case Boolean b -> {
                return Optional.of(b.toString());
            }
            case List<?> list -> {
                if (list.isEmpty()) {
                    return Optional.of("java.util.List.of()");
                }
            }
            default -> {
            }
        }
        return Optional.empty();
    }
    private List<ParameterSpec> strippedParams(List<ParameterSpec> fields) {
        return fields.stream()
                .map(f -> ParameterSpec.builder(f.type(), f.name()).build())
                .toList();
    }
    private Optional<List<ParameterSpec>> buildFields(Schema schema, String defaultPackage,
            List<UnionTypeGroup> pendingUnions) {
        var fields = new ArrayList<ParameterSpec>();
        for (var field : schema.getFields()) {
            var spec = buildField(field, defaultPackage, pendingUnions);
            if (spec == null) return Optional.empty();
            fields.add(spec);
        }
        return Optional.of(fields);
    }
    private ParameterSpec buildField(Schema.Field field, String defaultPackage,
            List<UnionTypeGroup> pendingUnions) {
        var resolution = resolveFieldSchema(field, defaultPackage, pendingUnions);
        if (resolution == null) return null;
        TypeName typeName;
        if (resolution.sealedInterface() != null) {
            typeName = resolution.sealedInterface();
        } else {
            typeName = toTypeName(resolution.schema(), defaultPackage, resolution.nullable());
            if (typeName == null) return null;
        }
        var paramBuilder = ParameterSpec.builder(typeName, field.name());
        if (resolution.nullable()) {
            paramBuilder.addAnnotation(Nullable.class);
        }
        sampleOf(field).ifPresent(sample -> paramBuilder.addAnnotation(exampleAnnotation(sample)));
        docOf(field).ifPresent(doc -> paramBuilder.addAnnotation(docAnnotation(doc)));
        return paramBuilder.build();
    }
    private FieldResolution resolveFieldSchema(Schema.Field field, String defaultPackage,
            List<UnionTypeGroup> pendingUnions) {
        var fieldSchema = field.schema();
        if (fieldSchema.getType() != Schema.Type.UNION) {
            return new FieldResolution(fieldSchema, false, null);
        }
        return resolveUnionSchema(field, fieldSchema, defaultPackage, pendingUnions);
    }
    /** Returns the effective non-null schema for a field, unwrapping single-branch nullable unions. */
    @Nullable
    private Schema resolvedNonNullSchema(Schema schema) {
        if (schema.getType() != Schema.Type.UNION) {
            return schema;
        }
        var nonNullTypes = schema.getTypes().stream()
                .filter(t -> t.getType() != Schema.Type.NULL)
                .toList();
        return nonNullTypes.size() == 1 ? nonNullTypes.getFirst() : null;
    }
    private FieldResolution resolveUnionSchema(Schema.Field field, Schema unionSchema, String defaultPackage,
            List<UnionTypeGroup> pendingUnions) {
        var hasNull = unionSchema.getTypes().stream().anyMatch(t -> t.getType() == Schema.Type.NULL);
        var nonNullTypes = unionSchema.getTypes().stream()
                .filter(t -> t.getType() != Schema.Type.NULL)
                .toList();
        if (nonNullTypes.size() == 1) {
            return new FieldResolution(nonNullTypes.getFirst(), hasNull, null);
        }
        // Multi-branch union: generate a sealed interface with one wrapper record per branch
        return buildMultiBranchUnion(field.name(), nonNullTypes, hasNull, defaultPackage, pendingUnions);
    }
    private FieldResolution buildMultiBranchUnion(String fieldName, List<Schema> nonNullBranches, boolean nullable,
            String defaultPackage, List<UnionTypeGroup> pendingUnions) {
        var sealedName = capitalize(fieldName);
        var branches = new ArrayList<UnionBranchRecord>();
        for (var branchSchema : nonNullBranches) {
            var suffix = branchTypeSuffix(branchSchema);
            if (suffix == null) return null;
            var branchName = sealedName + suffix;
            var valueType = branchValueType(branchSchema, defaultPackage);
            if (valueType == null) return null;
            var branchSpec = buildUnionBranchRecord(branchName, valueType,
                    ClassName.get(defaultPackage, sealedName));
            branches.add(new UnionBranchRecord(branchName, branchSpec));
        }
        var permittedSubclasses = branches.stream()
                .map(b -> (TypeName) ClassName.get(defaultPackage, b.name()))
                .toList();
        var interfaceSpec = buildUnionSealedInterface(sealedName, permittedSubclasses);
        pendingUnions.add(new UnionTypeGroup(sealedName, interfaceSpec, branches));
        return new FieldResolution(null, nullable, ClassName.get(defaultPackage, sealedName));
    }
    private TypeSpec buildUnionSealedInterface(String name, List<TypeName> permittedSubclasses) {
        var builder = TypeSpec.interfaceBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED);
        permittedSubclasses.forEach(builder::addPermittedSubclass);
        return builder.build();
    }
    private TypeSpec buildUnionBranchRecord(String branchName, TypeName valueType, ClassName sealedInterface) {
        return TypeSpec.recordBuilder(branchName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(sealedInterface)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameter(ParameterSpec.builder(valueType, "value").build())
                        .build())
                .build();
    }
    /** Returns the simple-name suffix used to name the wrapper record for this Avro union branch. */
    @Nullable
    private String branchTypeSuffix(Schema schema) {
        return switch (schema.getType()) {
            case STRING -> "String";
            case INT -> "Int";
            case LONG -> "Long";
            case DOUBLE -> "Double";
            case FLOAT -> "Float";
            case BOOLEAN -> "Boolean";
            case RECORD, ENUM -> capitalize(schema.getName());
            case ARRAY -> {
                var elementSuffix = branchTypeSuffix(schema.getElementType());
                yield elementSuffix != null ? elementSuffix + "List" : null;
            }
            default -> {
                reportError("Unsupported union branch type: " + schema.getType());
                yield null;
            }
        };
    }
    /** Returns the Java type for the {@code value} field of a union branch wrapper record. */
    @Nullable
    private TypeName branchValueType(Schema schema, String defaultPackage) {
        var logicalTypeName = resolveLogicalTypeName(schema);
        if (logicalTypeName != null) {
            return toLogicalTypeName(logicalTypeName);
        }
        return switch (schema.getType()) {
            case STRING -> ClassName.get(String.class);
            case INT -> TypeName.INT;
            case LONG -> TypeName.LONG;
            case DOUBLE -> TypeName.DOUBLE;
            case FLOAT -> TypeName.FLOAT;
            case BOOLEAN -> TypeName.BOOLEAN;
            case RECORD, ENUM -> ClassName.get(defaultPackage, capitalize(schema.getName()));
            case ARRAY -> {
                var elementType = branchValueType(schema.getElementType(), defaultPackage);
                if (elementType == null) yield null;
                var boxed = elementType.isPrimitive() ? elementType.box() : elementType;
                yield ParameterizedTypeName.get(ClassName.get(List.class), boxed);
            }
            default -> {
                reportError("Unsupported Avro type in union branch: " + schema.getType());
                yield null;
            }
        };
    }
    private Optional<String> sampleOf(Schema.Field field) {
        var fieldLevelSample = field.getProp(PROP_SAMPLE);
        if (fieldLevelSample != null) return Optional.of(fieldLevelSample);
        var schemaLevelSample = field.schema().getProp(PROP_SAMPLE);
        if (schemaLevelSample != null) return Optional.of(schemaLevelSample);
        if (field.schema().getType() == Schema.Type.UNION) {
            var nonNull = field.schema().getTypes().stream()
                    .filter(t -> t.getType() != Schema.Type.NULL)
                    .findFirst();
            return nonNull.map(s -> s.getProp(PROP_SAMPLE));
        }
        return Optional.empty();
    }
    private AnnotationSpec exampleAnnotation(String sample) {
        return AnnotationSpec.builder(Example.class)
                .addMember("value", "$S", sample)
                .build();
    }
    private Optional<String> docOf(Schema.Field field) {
        return Optional.ofNullable(field.doc());
    }
    private Optional<String> docOf(Schema schema) {
        return Optional.ofNullable(schema.getDoc());
    }
    private AnnotationSpec docAnnotation(String doc) {
        return AnnotationSpec.builder(Doc.class)
                .addMember("value", "$S", doc)
                .build();
    }
    private Optional<AnnotationSpec> avroSchemaAnnotation(Schema schema) {
        var javaName = javaTypeName(schema);
        var avroName = schema.getName();
        var namespace = schema.getNamespace();
        var hasNamespace = namespace != null && !namespace.isBlank();
        var hasNameOverride = !avroName.equals(javaName);
        if (!hasNamespace && !hasNameOverride) {
            return Optional.empty();
        }
        var builder = AnnotationSpec.builder(AvroSchema.class);
        if (hasNamespace) {
            builder.addMember("namespace", "$S", namespace);
        }
        if (hasNameOverride) {
            builder.addMember("name", "$S", avroName);
        }
        return Optional.of(builder.build());
    }
    private AnnotationSpec buildEventAnnotation(String[] topics, Event.Platform platform) {
        var builder = AnnotationSpec.builder(Event.class);
        if (topics.length == 1) {
            builder.addMember("topic", "$S", topics[0]);
        } else {
            var topicsBlock = stream(topics)
                    .map(t -> CodeBlock.of("$S", t))
                    .collect(joining(", "));
            builder.addMember("topic", "{$L}", topicsBlock);
        }
        builder.addMember("serialization", "$T.$L", ClassName.get(Event.Serialization.class), "AVRO");
        if (platform != null && platform != Event.Platform.DERIVED) {
            builder.addMember("platform", "$T.$L", ClassName.get(Event.Platform.class), platform.name());
        }
        return builder.build();
    }
    private TypeSpec buildEnum(Schema schema) {
        var typeName = javaTypeName(schema);
        var enumBuilder = TypeSpec.enumBuilder(typeName)
                .addModifiers(Modifier.PUBLIC);
        docOf(schema).ifPresent(doc -> enumBuilder.addAnnotation(docAnnotation(doc)));
        avroSchemaAnnotation(schema).ifPresent(enumBuilder::addAnnotation);
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
            case RECORD, ENUM -> ClassName.get(defaultPackage, capitalize(schema.getName()));
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
    private static String javaTypeName(Schema schema) {
        return capitalize(schema.getName());
    }
    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    private String builderSetterPrefix() {
        return processingEnvironment.getOptions().getOrDefault(OPTION_SETTER_PREFIX, "");
    }
    private void reportError(String message) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }
    private record FieldResolution(@Nullable Schema schema, boolean nullable, @Nullable ClassName sealedInterface) {}
    private record UnionTypeGroup(String interfaceName, TypeSpec interfaceSpec, List<UnionBranchRecord> branches) {}
    private record UnionBranchRecord(String name, TypeSpec spec) {}
}
