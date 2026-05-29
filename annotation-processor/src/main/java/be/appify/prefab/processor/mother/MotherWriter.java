package be.appify.prefab.processor.mother;

import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.FileOutput;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final FileOutput fileWriter;
    private final FileOutput productionFileWriter;

    MotherWriter(PrefabContext context, Set<String> writtenTypes) {
        this.context = context;
        this.writtenTypes = writtenTypes;
        this.fileWriter = new OutputTargetFileOutput(context, null, OutputTarget.TEST);
        this.productionFileWriter = new OutputTargetFileOutput(context, null, OutputTarget.MAIN);
    }

    /**
     * Writes a Mother for a generated request record.
     * Generates an inner {@code MotherBuilder} that extends the record's own {@code Builder}.
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
        var motherBuilderRef = ClassName.get("", "MotherBuilder");

        var effectiveParams = params.stream()
                .map(p -> new EffectiveParam(p, effectiveTypeOf(p)))
                .toList();

        var innerBuilder = buildRequestMotherBuilderInnerClass(requestType.nestedClass(BUILDER), effectiveParams);

        var typeSpec = TypeSpec.classBuilder(motherName)
                .addModifiers(Modifier.PUBLIC)
                .addType(innerBuilder)
                .addMethod(requestMotherBuilderFactoryMethod(motherBuilderRef, effectiveParams))
                .addMethod(createFactoryMethod(requestType, typeName))
                .addMethod(createConsumerOverloadForRecord(requestType, typeName, motherBuilderRef))
                .build();

        fileWriter.writeFile(packageName, motherName, typeSpec);

        effectiveParams.stream()
                .map(EffectiveParam::effectiveType)
                .forEach(type -> writeNestedMothersFor(type, preferredElement, false));
    }

    /**
     * Writes a Mother for an @Event-annotated type (or its concrete subtype if sealed).
     */
    void writeEventMother(TypeManifest eventType, TypeElement preferredElement, boolean hasEmbeddedBuilder) {
        var qualifiedName = eventType.packageName() + "." + eventType.simpleName();
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }
        fileWriter.setPreferredElement(preferredElement);
        var simpleName = eventType.simpleName().replace(".", "");
        var motherName = simpleName + MOTHER_SUFFIX;
        var packageName = eventType.packageName();
        var fields = eventType.fields();
        var eventTypeName = (ClassName) eventType.asTypeName();
        var motherBuilderRef = ClassName.get("", "MotherBuilder");
        var builderType = resolveBuilderType(eventTypeName, packageName, simpleName, fields, hasEmbeddedBuilder);
        var innerBuilder = buildEventMotherBuilderInnerClass(builderType, fields, hasEmbeddedBuilder);
        var typeSpec = TypeSpec.classBuilder(motherName)
                .addModifiers(Modifier.PUBLIC)
                .addType(innerBuilder)
                .addMethod(eventMotherBuilderFactoryMethod(motherBuilderRef, fields))
                .addMethod(motherFactoryMethod(eventTypeName, simpleName))
                .addMethod(motherConsumerOverload(eventTypeName, simpleName, motherBuilderRef))
                .build();
        fileWriter.writeFile(packageName, motherName, typeSpec);
        fields.forEach(f -> writeNestedMothersFor(f.type(), preferredElement, hasEmbeddedBuilder));
    }
    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    private ClassName resolveBuilderType(
            ClassName eventTypeName,
            String packageName,
            String simpleName,
            List<VariableManifest> fields,
            boolean hasEmbeddedBuilder
    ) {
        if (hasEmbeddedBuilder) {
            return eventTypeName.nestedClass(BUILDER);
        }
        var builderName = simpleName + BUILDER;
        var builderType = ClassName.get(packageName, builderName);
        productionFileWriter.writeFile(packageName, builderName,
                buildStandaloneBuilderClass(builderType, eventTypeName, fields));
        return builderType;
    }
    private void writeNestedMother(TypeManifest type, TypeElement preferredElement, boolean hasEmbeddedBuilder) {
        var qualifiedName = type.packageName() + "." + type.simpleName();
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }
        fileWriter.setPreferredElement(preferredElement);
        var simpleName = type.simpleName().replace(".", "");
        var motherName = simpleName + MOTHER_SUFFIX;
        var packageName = type.packageName();
        var fields = type.fields();
        var typeName = (ClassName) type.asTypeName();
        var builderType = resolveBuilderType(typeName, packageName, simpleName, fields, hasEmbeddedBuilder);
        var motherBuilderRef = ClassName.get("", "MotherBuilder");
        var innerBuilder = buildEventMotherBuilderInnerClass(builderType, fields, hasEmbeddedBuilder);
        var typeSpec = TypeSpec.classBuilder(motherName)
                .addModifiers(Modifier.PUBLIC)
                .addType(innerBuilder)
                .addMethod(eventMotherBuilderFactoryMethod(motherBuilderRef, fields))
                .addMethod(motherFactoryMethod(type.asTypeName(), simpleName))
                .addMethod(motherConsumerOverload(type.asTypeName(), simpleName, motherBuilderRef))
                .build();
        fileWriter.writeFile(packageName, motherName, typeSpec);
        fields.forEach(f -> writeNestedMothersFor(f.type(), preferredElement, hasEmbeddedBuilder));
    }
    private void writeNestedMothersFor(TypeManifest type, TypeElement preferredElement, boolean hasEmbeddedBuilder) {
        if (isNestedObjectType(type)) {
            writeNestedMother(type, preferredElement, hasEmbeddedBuilder);
        } else if (isUnionInterfaceType(type)) {
            type.permittedSubtypes().forEach(subtype -> writeNestedMother(subtype, preferredElement, hasEmbeddedBuilder));
        } else if (type.is(List.class) && isNestedObjectType(type.parameters().getFirst())) {
            writeNestedMother(type.parameters().getFirst(), preferredElement, hasEmbeddedBuilder);
        } else if (type.isSingleValueType()) {
            writeNestedMothersFor(type.fields().getFirst().type(), preferredElement, hasEmbeddedBuilder);
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

    // -- MotherBuilder inner class generation --

    /**
     * Builds the {@code MotherBuilder} inner class for event/nested mothers.
     * It extends the standalone or embedded builder and adds {@code Consumer<NestedBuilder>}
     * overloads for each nested-record field.
     */
    private TypeSpec buildEventMotherBuilderInnerClass(
            ClassName parentBuilderType,
            List<VariableManifest> fields,
            boolean hasEmbeddedBuilder
    ) {
        var motherBuilderRef = ClassName.get("", "MotherBuilder");
        var parameterizedParent = ParameterizedTypeName.get(parentBuilderType, motherBuilderRef);
        var inner = TypeSpec.classBuilder("MotherBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(parameterizedParent);
        fields.stream()
                .filter(f -> isNestedObjectType(f.type()))
                .forEach(f -> {
                    inner.addMethod(nestedConsumerSetterForEventMotherBuilder(f, hasEmbeddedBuilder));
                    if (f.nullable()) {
                        inner.addMethod(withoutNullableFieldMethod(f.name(), f.type().asTypeName()));
                    }
                });
        fields.stream()
                .filter(f -> isListOfNestedObjectType(f.type()))
                .forEach(f -> {
                    inner.addMethod(listVarargsOverloadForEventMotherBuilder(f));
                    inner.addMethod(emptyListMethod(f.name()));
                    if (f.nullable()) {
                        inner.addMethod(withoutNullableFieldMethod(f.name(), f.type().asTypeName()));
                    }
                });
        return inner.build();
    }

    /**
     * Builds the {@code MotherBuilder} inner class for request-record mothers.
     * It extends the record's own {@code Builder} and adds {@code Consumer<NestedBuilder>}
     * overloads for each nested-record effective param.
     */
    private TypeSpec buildRequestMotherBuilderInnerClass(
            ClassName parentBuilderType,
            List<EffectiveParam> params
    ) {
        var motherBuilderRef = ClassName.get("", "MotherBuilder");
        var parameterizedParent = ParameterizedTypeName.get(parentBuilderType, motherBuilderRef);
        var inner = TypeSpec.classBuilder("MotherBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(parameterizedParent);
        params.stream()
                .filter(ep -> isNestedObjectType(ep.effectiveType()))
                .forEach(ep -> {
                    inner.addMethod(nestedConsumerSetterForRequestMotherBuilder(ep));
                    if (ep.param().nullable()) {
                        inner.addMethod(withoutNullableFieldMethod(ep.param().name(), ep.effectiveType().asTypeName()));
                    }
                });
        params.stream()
                .filter(ep -> isListOfNestedObjectType(ep.effectiveType()))
                .forEach(ep -> {
                    inner.addMethod(listVarargsOverloadForRequestMotherBuilder(ep));
                    inner.addMethod(emptyListMethod(ep.param().name()));
                    if (ep.param().nullable()) {
                        inner.addMethod(withoutNullableFieldMethod(ep.param().name(), ep.effectiveType().asTypeName()));
                    }
                });
        return inner.build();
    }

    private MethodSpec nestedConsumerSetterForEventMotherBuilder(VariableManifest field, boolean hasEmbeddedBuilder) {
        var nestedType = field.type();
        var simpleName = nestedType.simpleName().replace(".", "");
        var motherType = ClassName.get(nestedType.packageName(), simpleName + MOTHER_SUFFIX);
         var nestedMotherBuilderType = motherType.nestedClass("MotherBuilder");
        return consumerSetterMethod(setterMethodName(field.name()), motherType, capitalize(simpleName), nestedMotherBuilderType);
    }

    private MethodSpec nestedConsumerSetterForRequestMotherBuilder(EffectiveParam ep) {
        var nestedType = ep.effectiveType();
        var simpleName = nestedType.simpleName().replace(".", "");
        var motherType = ClassName.get(nestedType.packageName(), simpleName + MOTHER_SUFFIX);
        var nestedMotherBuilderType = motherType.nestedClass("MotherBuilder");
        return consumerSetterMethod(setterMethodName(ep.param().name()), motherType, capitalize(simpleName), nestedMotherBuilderType);
    }

    private boolean isListOfNestedObjectType(TypeManifest type) {
        return type.is(List.class)
                && !type.parameters().isEmpty()
                && isNestedObjectType(type.parameters().getFirst());
    }

    /**
     * Generates a {@code withoutX()} method that sets a nullable field to {@code null}, disambiguating
     * against the {@code Consumer} overload.
     *
     * <pre>
     *   public MotherBuilder withoutAddress() {
     *       address((Address) null);
     *       return this;
     *   }
     * </pre>
     */
    private MethodSpec withoutNullableFieldMethod(String fieldName, TypeName fieldTypeName) {
        var setterName = setterMethodName(fieldName);
        return MethodSpec.methodBuilder("without" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", "MotherBuilder"))
                .addStatement("$L(($T) null)", setterName, fieldTypeName)
                .addStatement("return this")
                .build();
    }

    /**
     * Generates a varargs {@code Consumer} overload for list-of-record fields on event mothers.
     *
     * <pre>
     *   public MotherBuilder items(Consumer&lt;ItemMother.MotherBuilder&gt;... itemsCustomisers) {
     *       items(Arrays.stream(itemsCustomisers).map(ItemMother::createItem).toList());
     *       return this;
     *   }
     * </pre>
     */
    private MethodSpec listVarargsOverloadForEventMotherBuilder(VariableManifest field) {
        var elementType = field.type().parameters().getFirst();
        var simpleName = elementType.simpleName().replace(".", "");
        var motherType = ClassName.get(elementType.packageName(), simpleName + MOTHER_SUFFIX);
        var nestedBuilderType = motherType.nestedClass("MotherBuilder");
        return listVarargsMethod(setterMethodName(field.name()), field.name(), motherType, capitalize(simpleName), nestedBuilderType);
    }

    private MethodSpec listVarargsOverloadForRequestMotherBuilder(EffectiveParam ep) {
        var elementType = ep.effectiveType().parameters().getFirst();
        var simpleName = elementType.simpleName().replace(".", "");
        var motherType = ClassName.get(elementType.packageName(), simpleName + MOTHER_SUFFIX);
        var nestedBuilderType = motherType.nestedClass("MotherBuilder");
        return listVarargsMethod(setterMethodName(ep.param().name()), ep.param().name(), motherType, capitalize(simpleName), nestedBuilderType);
    }

    /**
     * Builds the shared varargs {@code Consumer} overload for a list-of-record setter.
     */
    private MethodSpec listVarargsMethod(
            String setterName,
            String fieldName,
            ClassName motherType,
            String capitalizedSimpleName,
            ClassName nestedBuilderType
    ) {
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), nestedBuilderType);
        var arrayOfConsumers = ArrayTypeName.of(consumerType);
        var paramName = fieldName + "Customisers";
        return MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "varargs")
                        .build())
                .varargs(true)
                .returns(ClassName.get("", "MotherBuilder"))
                .addParameter(arrayOfConsumers, paramName)
                .addStatement("$L($T.stream($L).map($T::create$L).toList())",
                        setterName, Arrays.class, paramName, motherType, capitalizedSimpleName)
                .addStatement("return this")
                .build();
    }

    /**
     * Generates an {@code emptyX()} method that sets a list field to an empty list.
     *
     * <pre>
     *   public MotherBuilder emptyItems() {
     *       items(List.of());
     *       return this;
     *   }
     * </pre>
     */
    private MethodSpec emptyListMethod(String fieldName) {
        var setterName = setterMethodName(fieldName);
        return MethodSpec.methodBuilder("empty" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", "MotherBuilder"))
                .addStatement("$L($T.of())", setterName, List.class)
                .addStatement("return this")
                .build();
    }

    /**
     * Generates a method like:
     * <pre>
     *   public MotherBuilder name(Consumer&lt;PersonNameBuilder&gt; nameCustomiser) {
     *       name(PersonNameMother.createPersonName(nameCustomiser));
     *       return this;
     *   }
     * </pre>
     */
    private MethodSpec consumerSetterMethod(
            String setterName,
            ClassName motherType,
            String capitalizedSimpleName,
            ClassName nestedBuilderType
    ) {
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), nestedBuilderType);
        var paramName = setterName + "Customiser";
        return MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", "MotherBuilder"))
                .addParameter(consumerType, paramName)
                .addStatement("$L($T.create$L($L))", setterName, motherType, capitalizedSimpleName, paramName)
                .addStatement("return this")
                .build();
    }

    // -- Factory methods --

    /** Returns a {@code builder()} method using statement style (avoids chaining on parent return type). */
    private MethodSpec eventMotherBuilderFactoryMethod(ClassName motherBuilderRef, List<VariableManifest> fields) {
        var code = CodeBlock.builder().addStatement("var builder = new $T()", motherBuilderRef);
        fields.forEach(field ->
                code.addStatement("builder.$L($L)", setterMethodName(field.name()), defaultValueFor(field, field.type())));
        code.addStatement("return builder");
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(motherBuilderRef)
                .addCode(code.build())
                .build();
    }

    /** Returns a {@code builder()} method for request mothers, populating defaults via statements. */
    private MethodSpec requestMotherBuilderFactoryMethod(ClassName motherBuilderRef, List<EffectiveParam> params) {
        var code = CodeBlock.builder().addStatement("var builder = new $T()", motherBuilderRef);
        params.forEach(ep ->
                code.addStatement("builder.$L($L)", setterMethodName(ep.param().name()), defaultValueFor(ep.param(), ep.effectiveType())));
        code.addStatement("return builder");
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(motherBuilderRef)
                .addCode(code.build())
                .build();
    }

    private MethodSpec createFactoryMethod(ClassName recordType, String typeName) {
        return MethodSpec.methodBuilder("create" + capitalize(typeName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(recordType)
                .addStatement("return builder().build()")
                .build();
    }

    private MethodSpec createConsumerOverloadForRecord(ClassName recordType, String typeName, ClassName motherBuilderRef) {
        var consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), motherBuilderRef);
        return MethodSpec.methodBuilder("create" + capitalize(typeName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(recordType)
                .addParameter(consumerType, "customiser")
                .addStatement("var builder = builder()")
                .addStatement("customiser.accept(builder)")
                .addStatement("return builder.build()")
                .build();
    }

    /** Pairs an original {@link VariableManifest} with its effective (possibly unwrapped) {@link TypeManifest}. */
    record EffectiveParam(VariableManifest param, TypeManifest effectiveType) {}

    // -- Event / nested types: standalone Builder class in production source --

    private TypeSpec buildStandaloneBuilderClass(ClassName builderType, TypeName targetType, List<VariableManifest> fields) {
        var selfTypeName = TypeVariableName.get("SELF");
        var selfBound = ParameterizedTypeName.get(builderType, selfTypeName);
        var selfTypeVar = TypeVariableName.get("SELF", selfBound);

        var builder = TypeSpec.classBuilder(builderType.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(selfTypeVar);

        fields.forEach(field -> builder.addField(
                FieldSpec.builder(field.type().asTypeName(), field.name(), Modifier.PRIVATE).build()
        ));

        fields.forEach(field -> builder.addMethod(standaloneWithMethod(field.name(), field.type().asTypeName())));

        builder.addMethod(standaloneSelfMethod());
        builder.addMethod(buildMethod(targetType,
                fields.stream().map(VariableManifest::name).collect(Collectors.joining(", "))));

        return builder.build();
    }

    private MethodSpec standaloneWithMethod(String fieldName, TypeName fieldType) {
        return MethodSpec.methodBuilder(setterMethodName(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("SELF"))
                .addParameter(fieldType, fieldName)
                .addStatement("this.$1N = $1N", fieldName)
                .addStatement("return self()")
                .build();
    }

    private MethodSpec standaloneSelfMethod() {
        return MethodSpec.methodBuilder("self")
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .returns(TypeVariableName.get("SELF"))
                .addStatement("return (SELF) this")
                .build();
    }

    private String setterMethodName(String fieldName) {
        var prefix = context.builderSetterPrefix();
        return prefix.isEmpty() ? fieldName : prefix + capitalize(fieldName);
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
                .or(() -> innerExampleOf(param, type))
                .orElseGet(() -> typeDefaultValue(type, param.name()));
    }

    /**
     * When the param's declared type is a single-value wrapper and the outer param carries no
     * {@code @Example}, fall back to the example on the wrapper's inner field (if present).
     */
    private Optional<CodeBlock> innerExampleOf(VariableManifest param, TypeManifest effectiveType) {
        if (!param.type().isSingleValueType()) {
            return Optional.empty();
        }
        var innerField = param.type().fields().getFirst();
        return innerField.getAnnotation(Example.class)
                .map(example -> exampleLiteralFor(example.value().value(), effectiveType, param));
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
            var innerDefault = defaultValueFor(innerField, innerField.type().asBoxed());
            return CodeBlock.of("new $T($L)", type.asTypeName(), innerDefault);
        }
        if (isUnionInterfaceType(type)) {
            return defaultValueForSealedType(type, fieldName);
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
        if (type.isEnum()) {
            return CodeBlock.of("$T.$L", type.asTypeName(), value);
        }
        if (type.isSingleValueType()) {
            var innerField = type.fields().getFirst();
            var innerLiteral = tryExampleLiteralFor(value, innerField.type().asBoxed());
            if (innerLiteral != null) {
                return CodeBlock.of("new $T($L)", type.asTypeName(), innerLiteral);
            }
        }
        if (isUnionInterfaceType(type)) {
            return exampleLiteralForSealedType(value, type, param);
        }
        return CodeBlock.of("$S", value);
    }

    private boolean isUnionInterfaceType(TypeManifest type) {
        return type.isSealed() || !type.permittedSubtypes().isEmpty();
    }

    private CodeBlock defaultValueForSealedType(TypeManifest sealedType, String fieldName) {
        var firstPermittedSubtype = sealedType.permittedSubtypes().stream().findFirst();
        return firstPermittedSubtype.map(typeManifest -> defaultValueForPermittedSubtype(typeManifest, fieldName))
                .orElseGet(() -> CodeBlock.of("null"));
    }

    private CodeBlock defaultValueForPermittedSubtype(TypeManifest subtype, String fieldName) {
        if (!subtype.isSingleValueType()) {
            return CodeBlock.of("null");
        }
        var valueField = subtype.fields().getFirst();
        var innerDefault = typeDefaultValue(valueField.type().asBoxed(), fieldName);
        return CodeBlock.of("new $T($L)", subtype.asTypeName(), innerDefault);
    }

    private CodeBlock exampleLiteralForSealedType(String value, TypeManifest sealedType, VariableManifest param) {
        for (var subtype : sealedType.permittedSubtypes()) {
            if (!subtype.isSingleValueType()) {
                continue;
            }
            var valueField = subtype.fields().getFirst();
            var innerLiteral = tryExampleLiteralFor(value, valueField.type().asBoxed());
            if (innerLiteral != null) {
                return CodeBlock.of("new $T($L)", subtype.asTypeName(), innerLiteral);
            }
        }
        reportExampleParseError(value,
                "one of " + sealedType.permittedSubtypes().stream().map(TypeManifest::simpleName).collect(Collectors.joining(", ")),
                param);
        return defaultValueForSealedType(sealedType, param.name());
    }

    /**
     * Attempts to produce a {@link CodeBlock} literal for {@code value} as the given {@code type}.
     * Returns {@code null} when the value cannot be interpreted as that type.
     */
    private CodeBlock tryExampleLiteralFor(String value, TypeManifest type) {
        if (type.is(String.class)) return CodeBlock.of("$S", value);
        if (type.is(int.class) || type.is(Integer.class)) {
            try { return CodeBlock.of("$L", Integer.parseInt(value)); } catch (NumberFormatException ignored) { return null; }
        }
        if (type.is(long.class) || type.is(Long.class)) {
            try { return CodeBlock.of("$LL", Long.parseLong(value)); } catch (NumberFormatException ignored) { return null; }
        }
        if (type.is(double.class) || type.is(Double.class)) {
            try { return CodeBlock.of("$L", Double.parseDouble(value)); } catch (NumberFormatException ignored) { return null; }
        }
        if (type.is(float.class) || type.is(Float.class)) {
            try { return CodeBlock.of("$Lf", Float.parseFloat(value)); } catch (NumberFormatException ignored) { return null; }
        }
        if (type.is(boolean.class) || type.is(Boolean.class)) {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return CodeBlock.of("$L", value.toLowerCase());
            }
            return null;
        }
        if (type.isEnum()) return CodeBlock.of("$T.$L", type.asTypeName(), value);
        return null;
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
}

