package be.appify.prefab.processor.mother;

import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TestJavaFileWriter;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.springframework.web.multipart.MultipartFile;

import static org.apache.commons.text.WordUtils.capitalize;

/**
 * Generates Object Mother classes for request records and event types.
 */
class MotherWriter {

    private static final String MOTHER_SUFFIX = "Mother";
    private static final String BUILDER = "Builder";
    private static final String FALSE_LITERAL = "false";
    private static final String NOW_TEMPLATE = "$T.now()";

    private final PrefabContext context;
    private final Set<String> writtenTypes;
    private final TestJavaFileWriter fileWriter;
    private final JavaFileWriter productionFileWriter;

    MotherWriter(PrefabContext context, Set<String> writtenTypes) {
        this.context = context;
        this.writtenTypes = writtenTypes;
        this.fileWriter = new TestJavaFileWriter(context, null);
        this.productionFileWriter = new JavaFileWriter(context.processingEnvironment(), null);
    }

    /**
     * Writes a Mother for a generated request record.
     * Delegates to the record's own generated builder.
     */
    void writeRequestMother(
            String typeName,
            String packageName,
            List<VariableManifest> params,
            TypeElement preferredElement
    ) {
        var qualifiedName = packageName + ".application." + typeName;
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }

        fileWriter.setPreferredElement(preferredElement);

        var requestType = ClassName.get(packageName + ".application", typeName);
        var motherName = typeName + MOTHER_SUFFIX;

        var effectiveParams = params.stream()
                .map(p -> new EffectiveParam(p, effectiveTypeOf(p)))
                .toList();

        var typeSpec = TypeSpec.classBuilder(motherName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(recordBuilderPassthroughMethod(requestType))
                .addMethod(createFactoryMethod(requestType, typeName, effectiveParams))
                .addMethod(createConsumerOverloadForRecord(requestType, typeName, effectiveParams))
                .build();

        fileWriter.writeFile(packageName, motherName, typeSpec);

        effectiveParams.stream()
                .map(EffectiveParam::effectiveType)
                .forEach(type -> writeNestedMothersFor(type, preferredElement));
    }

    /**
     * Writes a Mother for an @Event-annotated type (or its concrete subtype if sealed).
     * Generates a standalone Builder in production source; the mother only defines defaults.
     */
    void writeEventMother(TypeManifest eventType, TypeElement preferredElement) {
        var qualifiedName = eventType.packageName() + "." + eventType.simpleName();
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }

        fileWriter.setPreferredElement(preferredElement);

        var simpleName = eventType.simpleName().replace(".", "");
        var motherName = simpleName + MOTHER_SUFFIX;
        var builderName = simpleName + BUILDER;
        var packageName = eventType.packageName();
        var fields = eventType.fields();
        var eventTypeName = eventType.asTypeName();
        var builderType = ClassName.get(packageName, builderName);

        productionFileWriter.writeFile(packageName, builderName, buildStandaloneBuilderClass(builderType, eventTypeName, fields));

        var typeSpec = TypeSpec.classBuilder(motherName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(builderFactoryMethodWithDefaults(builderType, fields))
                .addMethod(motherFactoryMethod(eventTypeName, simpleName))
                .addMethod(motherConsumerOverload(eventTypeName, simpleName, builderType))
                .build();

        fileWriter.writeFile(packageName, motherName, typeSpec);

        fields.forEach(f -> writeNestedMothersFor(f.type(), preferredElement));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void writeNestedMother(TypeManifest type, TypeElement preferredElement) {
        var qualifiedName = type.packageName() + "." + type.simpleName();
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }

        fileWriter.setPreferredElement(preferredElement);

        var simpleName = type.simpleName().replace(".", "");
        var motherName = simpleName + MOTHER_SUFFIX;
        var builderName = simpleName + BUILDER;
        var packageName = type.packageName();
        var fields = type.fields();
        var builderType = ClassName.get(packageName, builderName);

        productionFileWriter.writeFile(packageName, builderName, buildStandaloneBuilderClass(builderType, type.asTypeName(), fields));

        var typeSpec = TypeSpec.classBuilder(motherName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(builderFactoryMethodWithDefaults(builderType, fields))
                .addMethod(motherFactoryMethod(type.asTypeName(), simpleName))
                .addMethod(motherConsumerOverload(type.asTypeName(), simpleName, builderType))
                .build();

        fileWriter.writeFile(packageName, motherName, typeSpec);

        fields.forEach(f -> writeNestedMothersFor(f.type(), preferredElement));
    }

    private void writeNestedMothersFor(TypeManifest type, TypeElement preferredElement) {
        if (isNestedObjectType(type)) {
            writeNestedMother(type, preferredElement);
        } else if (type.is(List.class) && isNestedObjectType(type.parameters().getFirst())) {
            writeNestedMother(type.parameters().getFirst(), preferredElement);
        } else if (type.isSingleValueType()) {
            writeNestedMothersFor(type.fields().getFirst().type(), preferredElement);
        }
    }

    private TypeManifest effectiveTypeOf(VariableManifest param) {
        return param.type().isSingleValueType()
                ? param.type().fields().getFirst().type().asBoxed()
                : param.type();
    }

    private boolean isNestedObjectType(TypeManifest type) {
        if (type.isStandardType()) return false;
        if (type.isSingleValueType()) return false;
        if (type.isEnum()) return false;
        if (type.is(List.class)) return false;
        if (type.is(Instant.class) || type.is(LocalDate.class) || type.is(LocalDateTime.class) || type.is(Duration.class)) return false;
        if (type.is(BigDecimal.class)) return false;
        return type.isRecord();
    }

    // -- Request record: delegate to the record's own generated builder --

    private MethodSpec recordBuilderPassthroughMethod(ClassName recordType) {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(recordType.nestedClass(BUILDER))
                .addStatement("return $T.builder()", recordType)
                .build();
    }

    private MethodSpec createFactoryMethod(ClassName recordType, String typeName, List<EffectiveParam> params) {
        var fieldDefaults = fieldDefaultsFor(params);
        var body = CodeBlock.builder()
                .add(builderWithDefaultsSetup(recordType, fieldDefaults))
                .addStatement("return builder.build()")
                .build();
        return MethodSpec.methodBuilder("create" + capitalize(typeName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(recordType)
                .addCode(body)
                .build();
    }

    private MethodSpec createConsumerOverloadForRecord(ClassName recordType, String typeName, List<EffectiveParam> params) {
        var fieldDefaults = fieldDefaultsFor(params);
        var builderType = recordType.nestedClass(BUILDER);
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), builderType);
        var body = CodeBlock.builder()
                .add(builderWithDefaultsSetup(recordType, fieldDefaults))
                .addStatement("customiser.accept(builder)")
                .addStatement("return builder.build()")
                .build();
        return MethodSpec.methodBuilder("create" + capitalize(typeName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(recordType)
                .addParameter(consumerType, "customiser")
                .addCode(body)
                .build();
    }

    private List<FieldDefault> fieldDefaultsFor(List<EffectiveParam> params) {
        return params.stream()
                .map(ep -> new FieldDefault(ep.param().name(), defaultValueFor(ep.param(), ep.effectiveType())))
                .toList();
    }

    private CodeBlock builderWithDefaultsSetup(ClassName recordType, List<FieldDefault> fieldDefaults) {
        var block = CodeBlock.builder().add("var builder = $T.builder()", recordType);
        for (var fd : fieldDefaults) {
            block.add("\n        .$L($L)", setterMethodName(fd.name()), fd.defaultValue());
        }
        block.add(";\n");
        return block.build();
    }

    // -- Event / nested types: standalone Builder class in production source --

    private TypeSpec buildStandaloneBuilderClass(ClassName builderType, TypeName targetType, List<VariableManifest> fields) {
        var builder = TypeSpec.classBuilder(builderType.simpleName())
                .addModifiers(Modifier.PUBLIC);

        fields.forEach(field -> builder.addField(
                FieldSpec.builder(field.type().asTypeName(), field.name(), Modifier.PRIVATE).build()
        ));

        fields.forEach(field -> builder.addMethod(withMethod(field.name(), field.type().asTypeName(), builderType)));

        builder.addMethod(buildMethod(targetType,
                fields.stream().map(VariableManifest::name).collect(Collectors.joining(", "))));

        return builder.build();
    }

    private MethodSpec withMethod(String fieldName, TypeName fieldType, ClassName builderType) {
        return MethodSpec.methodBuilder(setterMethodName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(builderType)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$1N = $1N", fieldName)
                .addStatement("return this")
                .build();
    }

    private String setterMethodName(String fieldName) {
        var prefix = context.builderSetterPrefix();
        return prefix.isEmpty() ? fieldName : prefix + capitalize(fieldName);
    }

    private MethodSpec builderFactoryMethodWithDefaults(ClassName builderType, List<VariableManifest> fields) {
        var code = CodeBlock.builder().add("return new $T()", builderType);
        fields.forEach(field -> code.add("\n        .$L($L)", setterMethodName(field.name()), defaultValueFor(field, field.type())));
        code.add(";\n");
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderType)
                .addCode(code.build())
                .build();
    }

    private MethodSpec buildMethod(TypeName targetType, String args) {
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(targetType)
                .addStatement("return new $T($L)", targetType, args)
                .build();
    }

    private MethodSpec motherFactoryMethod(TypeName returnType, String typeName) {
        return MethodSpec.methodBuilder("create" + capitalize(typeName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType)
                .addStatement("return builder().build()")
                .build();
    }

    private MethodSpec motherConsumerOverload(TypeName returnType, String typeName, ClassName builderType) {
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), builderType);
        return MethodSpec.methodBuilder("create" + capitalize(typeName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType)
                .addParameter(consumerType, "customiser")
                .addStatement("var builder = builder()")
                .addStatement("customiser.accept(builder)")
                .addStatement("return builder.build()")
                .build();
    }

    // -- Default value computation --

    private CodeBlock defaultValueFor(VariableManifest param, TypeManifest type) {
        return param.getAnnotation(Example.class)
                .map(example -> exampleLiteralFor(example.value().value(), type, param))
                .orElseGet(() -> typeDefaultValue(type, param.name()));
    }

    private CodeBlock typeDefaultValue(TypeManifest type, String fieldName) {
        if (type.is(String.class)) return CodeBlock.of("$S", fieldName);
        var numericDefault = numericDefaultValue(type);
        if (numericDefault != null) return numericDefault;
        if (type.is(boolean.class) || type.is(Boolean.class)) return CodeBlock.of(FALSE_LITERAL);
        if (type.is(List.class)) {
            var elementType = type.parameters().getFirst();
            return CodeBlock.of("$T.of($L)", List.class, typeDefaultValue(elementType, fieldName));
        }
        if (type.is(Map.class)) {
            return CodeBlock.of("$T.of()", Map.class);
        }
        var temporalDefault = temporalDefaultValue(type);
        if (temporalDefault != null) return temporalDefault;
        if (type.is(BigDecimal.class)) return CodeBlock.of("$T.ONE", BigDecimal.class);
        if (type.is(MultipartFile.class)) {
            var mockMultipartFile = ClassName.get("org.springframework.mock.web", "MockMultipartFile");
            return CodeBlock.of("new $T($S, $S, $S, new byte[0])", mockMultipartFile, fieldName, fieldName, "application/octet-stream");
        }
        if (type.isEnum()) return CodeBlock.of("$T.values()[0]", type.asTypeName());
        if (type.isSingleValueType()) {
            var innerField = type.fields().getFirst();
            var innerDefault = typeDefaultValue(innerField.type().asBoxed(), innerField.name());
            return CodeBlock.of("new $T($L)", type.asTypeName(), innerDefault);
        }
        if (isNestedObjectType(type)) {
            var simpleName = type.simpleName().replace(".", "");
            var motherType = ClassName.get(type.packageName(), simpleName + MOTHER_SUFFIX);
            return CodeBlock.of("$T.create$L()", motherType, capitalize(simpleName));
        }
        return CodeBlock.of("null");
    }

    private static CodeBlock numericDefaultValue(TypeManifest type) {
        if (type.is(int.class) || type.is(Integer.class)) return CodeBlock.of("1");
        if (type.is(long.class) || type.is(Long.class)) return CodeBlock.of("1L");
        if (type.is(double.class) || type.is(Double.class)) return CodeBlock.of("1.0");
        if (type.is(float.class) || type.is(Float.class)) return CodeBlock.of("1.0f");
        return null;
    }

    private static CodeBlock temporalDefaultValue(TypeManifest type) {
        if (type.is(Instant.class)) return CodeBlock.of(NOW_TEMPLATE, Instant.class);
        if (type.is(LocalDate.class)) return CodeBlock.of(NOW_TEMPLATE, LocalDate.class);
        if (type.is(LocalDateTime.class)) return CodeBlock.of(NOW_TEMPLATE, LocalDateTime.class);
        if (type.is(Duration.class)) return CodeBlock.of("$T.ofSeconds(1)", Duration.class);
        return null;
    }

    private CodeBlock exampleLiteralFor(String value, TypeManifest type, VariableManifest param) {
        if (type.is(String.class)) return CodeBlock.of("$S", value);
        var numericLiteral = numericExampleLiteral(value, type, param);
        if (numericLiteral != null) return numericLiteral;
        if (type.is(boolean.class) || type.is(Boolean.class)) {
            if (!"true".equalsIgnoreCase(value) && !FALSE_LITERAL.equalsIgnoreCase(value)) {
                reportExampleParseError(value, "boolean", param);
                return CodeBlock.of(FALSE_LITERAL);
            }
            return CodeBlock.of("$L", value.toLowerCase());
        }
        return CodeBlock.of("$S", value);
    }

    private CodeBlock numericExampleLiteral(String value, TypeManifest type, VariableManifest param) {
        if (type.is(int.class) || type.is(Integer.class)) {
            return parsedNumericLiteral(value, "int", param, () ->
                    CodeBlock.of("$L", Integer.parseInt(value)), CodeBlock.of("1"));
        }
        if (type.is(long.class) || type.is(Long.class)) {
            return parsedNumericLiteral(value, "long", param, () ->
                    CodeBlock.of("$LL", Long.parseLong(value)), CodeBlock.of("1L"));
        }
        if (type.is(double.class) || type.is(Double.class)) {
            return parsedNumericLiteral(value, "double", param, () ->
                    CodeBlock.of("$L", Double.parseDouble(value)), CodeBlock.of("1.0"));
        }
        if (type.is(float.class) || type.is(Float.class)) {
            return parsedNumericLiteral(value, "float", param, () ->
                    CodeBlock.of("$Lf", Float.parseFloat(value)), CodeBlock.of("1.0f"));
        }
        return null;
    }

    private CodeBlock parsedNumericLiteral(
            String value,
            String targetType,
            VariableManifest param,
            java.util.concurrent.Callable<CodeBlock> parser,
            CodeBlock fallback
    ) {
        try {
            return parser.call();
        } catch (Exception e) {
            reportExampleParseError(value, targetType, param);
            return fallback;
        }
    }

    private void reportExampleParseError(String value, String targetType, VariableManifest param) {
        context.processingEnvironment().getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "@Example value \"" + value + "\" cannot be parsed as " + targetType
                        + " on field \"" + param.name() + "\"",
                param.element()
        );
    }

    private record FieldDefault(String name, CodeBlock defaultValue) {}

    /** Pairs an original {@link VariableManifest} with its effective (possibly unwrapped) {@link TypeManifest}. */
    record EffectiveParam(VariableManifest param, TypeManifest effectiveType) {}
}
