package be.appify.prefab.processor.assertion;

import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.SourceContributions;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Generates AssertJ-style assertion classes for response records, event types, and nested records.
 * Each generated class extends {@code AbstractAssert} and exposes one {@code has{Field}()} method
 * per record component, plus a static {@code assertThat()} factory method.
 */
class AssertionWriter {

    private static final ClassName ABSTRACT_ASSERT = ClassName.get("org.assertj.core.api", "AbstractAssert");
    private static final ClassName ASSERTIONS = ClassName.get("org.assertj.core.api", "Assertions");
    private static final ClassName LIST_ASSERT = ClassName.get("org.assertj.core.api", "ListAssert");
    private static final ClassName CONSUMER = ClassName.get("java.util.function", "Consumer");

    private final PrefabContext context;
    private final Set<String> writtenTypes;
    private final SourceContributions<AssertionEntry> entries;
    private final OutputTargetFileOutput fileWriter;

    AssertionWriter(PrefabContext context, Set<String> writtenTypes, SourceContributions<AssertionEntry> entries) {
        this.context = context;
        this.writtenTypes = writtenTypes;
        this.entries = entries;
        this.fileWriter = new OutputTargetFileOutput(context, null, OutputTarget.TEST);
    }

    void writeResponseAssert(ClassManifest manifest) {
        fileWriter.setPreferredElement(manifest.type().asElement());
        var responsePackage = manifest.packageName() + ".infrastructure.http";
        var responseTypeName = manifest.simpleName() + "Response";
        var subjectType = ClassName.get(responsePackage, responseTypeName);
        var assertName = responseTypeName + "Assert";
        var assertFields = Stream.concat(
                        manifest.fields().stream(),
                        manifest.methodsWith(Computed.class).stream()
                                .map(method -> VariableManifest.ofMethod(method, context.processingEnvironment())))
                .toList();
        if (writeAssertClass(responsePackage, assertName, subjectType, assertFields, manifest.type().asElement())) {
            manifest.fields().forEach(field -> writeNestedAssertFor(field.type(), manifest.type().asElement()));
            manifest.methodsWith(Computed.class).stream()
                    .map(method -> TypeManifest.of(method.getReturnType(), context.processingEnvironment()))
                    .forEach(type -> writeNestedAssertFor(type, manifest.type().asElement()));
        }
    }

    void writeEventAssert(TypeElement element) {
        fileWriter.setPreferredElement(element);
        var type = TypeManifest.of(element.asType(), context.processingEnvironment());
        if (type.isSealed()) {
            type.permittedSubtypes().forEach(subtype -> {
                if (subtype.isRecord()) {
                    writeEventTypeAssert(subtype, element);
                }
            });
        } else if (type.isRecord()) {
            writeEventTypeAssert(type, element);
        }
    }

    void writeAssertionsClass(String packageName, List<AssertionEntry> entries, TypeElement[] owners) {
        var qualifiedName = fileWriter.target() + ":" + packageName + ".Assertions";
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }
        var typeSpec = TypeSpec.classBuilder("Assertions")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(privateConstructor())
                .addMethods(entries.stream()
                        .map(entry -> assertThatDelegateMethod(entry.subjectType(), ClassName.get(packageName, entry.assertName())))
                        .toList())
                .build();
        fileWriter.setOriginatingElements(owners);
        fileWriter.writeFile(packageName, "Assertions", typeSpec);
    }

    private void writeEventTypeAssert(TypeManifest eventType, TypeElement preferredElement) {
        if (!(eventType.asTypeName() instanceof ClassName subjectType)) {
            return;
        }
        fileWriter.setPreferredElement(preferredElement);
        var flatName = eventType.simpleName().replace(".", "");
        var packageName = eventType.packageName();
        var assertName = flatName + "Assert";
        var assertFields = Stream.concat(
                        eventType.fields().stream(),
                        eventType.methodsWith(Computed.class).stream()
                                .map(method -> VariableManifest.ofMethod(method, context.processingEnvironment())))
                .toList();
        if (writeAssertClass(packageName, assertName, subjectType, assertFields, preferredElement)) {
            eventType.fields().forEach(field -> writeNestedAssertFor(field.type(), preferredElement));
            eventType.methodsWith(Computed.class).stream()
                    .map(method -> TypeManifest.of(method.getReturnType(), context.processingEnvironment()))
                    .forEach(type -> writeNestedAssertFor(type, preferredElement));
        }
    }

    private void writeNestedAssertFor(TypeManifest type, TypeElement preferredElement) {
        if (isNestedRecordType(type)) {
            var subjectType = eraseParameters(type.asTypeName());
            var flatName = type.simpleName().replace(".", "");
            var packageName = type.packageName();
            var assertName = flatName + "Assert";
            var assertFields = Stream.concat(
                            type.fields().stream(),
                            type.methodsWith(Computed.class).stream()
                                    .map(method -> VariableManifest.ofMethod(method, context.processingEnvironment())))
                    .toList();
            if (writeAssertClass(packageName, assertName, subjectType, assertFields, preferredElement)) {
                type.fields().forEach(field -> writeNestedAssertFor(field.type(), preferredElement));
                type.methodsWith(Computed.class).stream()
                        .map(method -> TypeManifest.of(method.getReturnType(), context.processingEnvironment()))
                        .forEach(fieldType -> writeNestedAssertFor(fieldType, preferredElement));
            }
        } else if (type.is(List.class) && !type.parameters().isEmpty()
                && isNestedRecordType(type.parameters().getFirst())) {
            writeNestedAssertFor(type.parameters().getFirst(), preferredElement);
        }
    }

    private TypeName eraseParameters(TypeName typeName) {
        if(typeName instanceof ParameterizedTypeName parameterizedTypeName) {
            return ParameterizedTypeName.get(parameterizedTypeName.rawType(),
                    parameterizedTypeName.typeArguments().stream()
                            .map(parameter -> WildcardTypeName.subtypeOf(Object.class))
                            .toArray(TypeName[]::new));
        }
        return typeName;
    }

    private boolean writeAssertClass(
            String packageName,
            String assertName,
            TypeName subjectType,
            List<VariableManifest> fields,
            TypeElement preferredElement
    ) {
        var qualifiedName = fileWriter.target() + ":" + packageName + "." + assertName;
        var raw = subjectType instanceof ParameterizedTypeName p ? p.rawType() : (ClassName) subjectType;
        int arity = subjectType instanceof ParameterizedTypeName p ? p.typeArguments().size() : 0;
        entries.add(preferredElement, new AssertionEntry(packageName, raw.simpleNames(), arity, assertName, fileWriter.target()));
        // Visit the nested graph once per contributing source, even when another source emitted the helper.
        if (!writtenTypes.add(qualifiedName + ":" + preferredElement.getQualifiedName())) {
            return false;
        }
        if (!writtenTypes.add(qualifiedName)) {
            fileWriter.alsoOwnedBy(packageName, assertName, preferredElement);
            return true;
        }
        var assertType = ClassName.get(packageName, assertName);
        var selfTypePlaceholder = TypeVariableName.get("SELF");
        var selfType = TypeVariableName.get("SELF", ParameterizedTypeName.get(assertType, selfTypePlaceholder));
        var typeSpec = TypeSpec.classBuilder(assertName)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(selfType)
                .superclass(ParameterizedTypeName.get(ABSTRACT_ASSERT, selfType, subjectType))
                .addMethod(staticAssertThatFactory(assertType, subjectType))
                .addMethod(protectedConstructorFor(assertType, subjectType))
                .addMethods(fieldAssertMethods(selfType, fields))
                .build();
        fileWriter.setPreferredElement(preferredElement);
        fileWriter.writeFile(packageName, assertName, typeSpec);
        return true;
    }

    private MethodSpec privateConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec staticAssertThatFactory(ClassName assertType, TypeName subjectType) {
        return MethodSpec.methodBuilder("assertThat")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(assertType, WildcardTypeName.subtypeOf(Object.class)))
                .addParameter(subjectType, "actual")
                .addStatement("return new $T<>(actual)", assertType)
                .build();
    }

    private MethodSpec protectedConstructorFor(ClassName assertType, TypeName subjectType) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(subjectType, "actual")
                .addStatement("super(actual, $T.class)", assertType)
                .build();
    }

    private List<MethodSpec> fieldAssertMethods(TypeVariableName selfType, List<VariableManifest> fields) {
        return fields.stream()
                .flatMap(field -> fieldAssertMethods(selfType, field).stream())
                .toList();
    }

    private List<MethodSpec> fieldAssertMethods(TypeVariableName selfType, VariableManifest field) {
        if (field.type().is(List.class)) {
            return listFieldAssertMethods(selfType, field);
        }
        if (isSingleValueRecordField(field)) {
            var methods = new ArrayList<MethodSpec>();
            methods.add(singleValueRecordFieldAssertMethod(selfType, field));
            methods.add(recordFieldAssertMethod(selfType, field));
            if (field.nullable()) {
                methods.add(recordFieldNullAssertMethod(selfType, field));
            }
            return List.copyOf(methods);
        }
        if (isNestedRecordType(field.type())) {
            if (field.nullable()) {
                return List.of(recordFieldAssertMethod(selfType, field), recordFieldNullAssertMethod(selfType, field));
            }
            return List.of(recordFieldAssertMethod(selfType, field));
        }
        return List.of(equalsFieldAssertMethod(selfType, field));
    }

    private List<MethodSpec> listFieldAssertMethods(TypeVariableName selfType, VariableManifest field) {
        var methods = new ArrayList<MethodSpec>();
        methods.add(listFieldAssertMethod(selfType, field));
        if (isListWithNestedRecordElement(field)) {
            methods.add(listElementAssertMethod(selfType, field));
            methods.add(listElementsWithAssertMethod(selfType, field));
        }
        return List.copyOf(methods);
    }

    private boolean isListWithNestedRecordElement(VariableManifest field) {
        return !field.type().parameters().isEmpty()
                && isNestedRecordType(field.type().parameters().getFirst());
    }

    private MethodSpec listElementsWithAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var elementType = field.type().parameters().getFirst();
        var flatName = elementType.simpleName().replace(".", "");
        var packageName = elementType.packageName();
        var elementAssertType = ClassName.get(packageName, flatName + "Assert");
        var wildcardedElementAssertType = ParameterizedTypeName.get(elementAssertType, WildcardTypeName.subtypeOf(Object.class));
        var consumerType = ParameterizedTypeName.get(CONSUMER, wildcardedElementAssertType);
        var fieldName = field.name();
        return MethodSpec.methodBuilder("has" + capitalize(fieldName) + "With")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(SafeVarargs.class)
                .returns(selfType)
                .addParameter(com.palantir.javapoet.ArrayTypeName.of(consumerType), "requirements")
                .varargs(true)
                .addStatement("isNotNull()")
                .addStatement("$T.requireNonNull(requirements, $S)", Objects.class, "requirements must not be null")
                .addStatement("$T.assertThat(actual.$N()).hasSize(requirements.length)", ASSERTIONS, fieldName)
                .beginControlFlow("for (int i = 0; i < requirements.length; i++)")
                .addStatement("requirements[i].accept($T.assertThat(actual.$N().get(i)))", elementAssertType, fieldName)
                .endControlFlow()
                .addStatement("return myself")
                .build();
    }

    private MethodSpec listElementAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var elementType = field.type().parameters().getFirst();
        var flatName = elementType.simpleName().replace(".", "");
        var packageName = elementType.packageName();
        var elementAssertType = ClassName.get(packageName, flatName + "Assert");
        var wildcardedElementAssertType = ParameterizedTypeName.get(elementAssertType, WildcardTypeName.subtypeOf(Object.class));
        var consumerType = ParameterizedTypeName.get(CONSUMER, wildcardedElementAssertType);
        return MethodSpec.methodBuilder("has" + capitalize(flatName) + "Satisfying")
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(consumerType, "requirements")
                .addStatement("isNotNull()")
                .addStatement("$T.requireNonNull(requirements, $S)", Objects.class, "requirements must not be null")
                .addStatement("$T.assertThat(actual.$N()).anySatisfy(element -> requirements.accept($T.assertThat(element)))",
                        ASSERTIONS, field.name(), elementAssertType)
                .addStatement("return myself")
                .build();
    }

    private MethodSpec recordFieldAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var fieldName = field.name();
        var fieldType = field.type();
        var flatName = fieldType.simpleName().replace(".", "");
        var packageName = fieldType.packageName();
        var fieldAssertType = ClassName.get(packageName, flatName + "Assert");
        var wildcardedFieldAssertType = ParameterizedTypeName.get(fieldAssertType, WildcardTypeName.subtypeOf(Object.class));
        var consumerType = ParameterizedTypeName.get(CONSUMER, wildcardedFieldAssertType);
        return MethodSpec.methodBuilder("has" + capitalize(fieldName) + "Satisfying")
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(consumerType, "requirements")
                .addStatement("isNotNull()")
                .addStatement("$T.requireNonNull(requirements, $S)", Objects.class, "requirements must not be null")
                .addStatement("requirements.accept($T.assertThat(actual.$N()))", fieldAssertType, fieldName)
                .addStatement("return myself")
                .build();
    }

    private MethodSpec recordFieldNullAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var fieldName = field.name();
        return MethodSpec.methodBuilder("has" + capitalize(fieldName) + "Null")
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addStatement("isNotNull()")
                .beginControlFlow("if (actual.$N() != null)", fieldName)
                .addStatement("failWithMessage($S, actual.$N())",
                        "Expected " + fieldName + " to be <null> but was <%s>", fieldName)
                .endControlFlow()
                .addStatement("return myself")
                .build();
    }

    private MethodSpec singleValueRecordFieldAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var fieldName = field.name();
        var innerField = field.type().fields().getFirst();
        var innerType = innerField.type().asBoxed().asTypeName();
        var innerAccessor = innerField.name();
        return MethodSpec.methodBuilder("has" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(innerType, "expected")
                .addStatement("isNotNull()")
                .addStatement("$T.assertThat(actual.$N()).isNotNull()", ASSERTIONS, fieldName)
                .beginControlFlow("if (!$T.equals(actual.$N().$N(), expected))", Objects.class, fieldName, innerAccessor)
                .addStatement("failWithMessage($S, expected, actual.$N().$N())",
                        "Expected " + fieldName + " to be <%s> but was <%s>", fieldName, innerAccessor)
                .endControlFlow()
                .addStatement("return myself")
                .build();
    }

    private boolean isSingleValueRecordField(VariableManifest field) {
        return field.type().isRecord() && field.type().isSingleValueType();
    }

    private MethodSpec equalsFieldAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var fieldName = field.name();
        var paramType = field.type().asBoxed().asTypeName();
        return MethodSpec.methodBuilder("has" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(paramType, "expected")
                .addStatement("isNotNull()")
                .beginControlFlow("if (!$T.equals(actual.$N(), expected))", Objects.class, fieldName)
                .addStatement("failWithMessage($S, expected, actual.$N())",
                        "Expected " + fieldName + " to be <%s> but was <%s>", fieldName)
                .endControlFlow()
                .addStatement("return myself")
                .build();
    }

    private MethodSpec listFieldAssertMethod(TypeVariableName selfType, VariableManifest field) {
        var fieldName = field.name();
        var listElementType = listElementTypeOf(field);
        var listAssertType = ParameterizedTypeName.get(LIST_ASSERT, listElementType);
        var requirementsType = ParameterizedTypeName.get(CONSUMER, listAssertType);
        return MethodSpec.methodBuilder("has" + capitalize(fieldName) + "Satisfying")
                .addModifiers(Modifier.PUBLIC)
                .returns(selfType)
                .addParameter(requirementsType, "requirements")
                .addStatement("isNotNull()")
                .addStatement("$T.requireNonNull(requirements, $S)", Objects.class, "requirements must not be null")
                .addStatement("$T actualListAssert = $T.assertThat(actual.$N())", listAssertType, ASSERTIONS, fieldName)
                .addStatement("requirements.accept(actualListAssert)")
                .addStatement("return myself")
                .build();
    }

    private TypeName listElementTypeOf(VariableManifest field) {
        if (field.type().parameters().isEmpty()) {
            return TypeName.get(Object.class);
        }
        return field.type().parameters().getFirst().asBoxed().asTypeName();
    }

    private MethodSpec assertThatDelegateMethod(TypeName subjectType, ClassName assertType) {
        return MethodSpec.methodBuilder("assertThat")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(assertType, WildcardTypeName.subtypeOf(Object.class)))
                .addParameter(subjectType, "actual")
                .addStatement("return $T.assertThat(actual)", assertType)
                .build();
    }

    private boolean isNestedRecordType(TypeManifest type) {
        if (!type.isRecord()) return false;
        if (type.isStandardType()) return false;
        if (type.isCustomType()) return false;
        if (type.is(Instant.class) || type.is(LocalDate.class) || type.is(LocalDateTime.class)
                || type.is(Duration.class)) return false;
        return !type.is(BigDecimal.class);
    }

    record AssertionEntry(String packageName, List<String> simpleNames, int arity, String assertName, OutputTarget target) {
        TypeName subjectType() {
            var raw = ClassName.get(packageName, simpleNames.getFirst(), simpleNames.stream().skip(1).toArray(String[]::new));
            return arity == 0 ? raw : ParameterizedTypeName.get(raw,
                    Collections.nCopies(arity, WildcardTypeName.subtypeOf(Object.class)).toArray(TypeName[]::new));
        }
    }
}
