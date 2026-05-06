package be.appify.prefab.processor.assertion;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TestJavaFileWriter;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final Map<String, List<AssertionEntry>> entriesByPackage;
    private final TestJavaFileWriter fileWriter;

    AssertionWriter(PrefabContext context, Set<String> writtenTypes, Map<String, List<AssertionEntry>> entriesByPackage) {
        this.context = context;
        this.writtenTypes = writtenTypes;
        this.entriesByPackage = entriesByPackage;
        this.fileWriter = new TestJavaFileWriter(context, null);
    }

    void writeResponseAssert(ClassManifest manifest) {
        fileWriter.setPreferredElement(manifest.type().asElement());
        var responsePackage = manifest.packageName() + ".infrastructure.http";
        var responseTypeName = manifest.simpleName() + "Response";
        var subjectType = ClassName.get(responsePackage, responseTypeName);
        var assertName = responseTypeName + "Assert";
        if (writeAssertClass(responsePackage, assertName, subjectType, manifest.fields(), manifest.type().asElement())) {
            addEntry(responsePackage, subjectType, ClassName.get(responsePackage, assertName));
            manifest.fields().forEach(field -> writeNestedAssertFor(field.type(), manifest.type().asElement()));
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

    void writeAssertionsClass(String packageName, List<AssertionEntry> entries) {
        var qualifiedName = packageName + ".Assertions";
        if (!writtenTypes.add(qualifiedName)) {
            return;
        }
        var typeSpec = TypeSpec.classBuilder("Assertions")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(privateConstructor())
                .addMethods(entries.stream()
                        .map(entry -> assertThatDelegateMethod(entry.subjectType(), entry.assertType()))
                        .toList())
                .build();
        var preferredElement = findPreferredElement(packageName);
        preferredElement.ifPresent(fileWriter::setPreferredElement);
        fileWriter.writeFile(packageName, "Assertions", typeSpec);
    }

    private java.util.Optional<TypeElement> findPreferredElement(String packageName) {
        return context.roundEnvironment().getRootElements().stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .filter(e -> {
                    var qn = e.getQualifiedName().toString();
                    return qn.startsWith(packageName + ".");
                })
                .findFirst();
    }

    private void writeEventTypeAssert(TypeManifest eventType, TypeElement preferredElement) {
        if (!(eventType.asTypeName() instanceof ClassName subjectType)) {
            return;
        }
        fileWriter.setPreferredElement(preferredElement);
        var flatName = eventType.simpleName().replace(".", "");
        var packageName = eventType.packageName();
        var assertName = flatName + "Assert";
        if (writeAssertClass(packageName, assertName, subjectType, eventType.fields(), preferredElement)) {
            addEntry(packageName, subjectType, ClassName.get(packageName, assertName));
            eventType.fields().forEach(field -> writeNestedAssertFor(field.type(), preferredElement));
        }
    }

    private void writeNestedAssertFor(TypeManifest type, TypeElement preferredElement) {
        if (isNestedRecordType(type) && type.asTypeName() instanceof ClassName subjectType) {
            var flatName = type.simpleName().replace(".", "");
            var packageName = type.packageName();
            var assertName = flatName + "Assert";
            if (writeAssertClass(packageName, assertName, subjectType, type.fields(), preferredElement)) {
                addEntry(packageName, subjectType, ClassName.get(packageName, assertName));
                type.fields().forEach(field -> writeNestedAssertFor(field.type(), preferredElement));
            }
        } else if (type.is(List.class) && !type.parameters().isEmpty()
                && isNestedRecordType(type.parameters().getFirst())) {
            writeNestedAssertFor(type.parameters().getFirst(), preferredElement);
        }
    }

    private boolean writeAssertClass(
            String packageName,
            String assertName,
            ClassName subjectType,
            List<VariableManifest> fields,
            TypeElement preferredElement
    ) {
        var qualifiedName = packageName + "." + assertName;
        if (!writtenTypes.add(qualifiedName)) {
            return false;
        }
        var assertType = ClassName.get(packageName, assertName);
        var typeSpec = TypeSpec.classBuilder(assertName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(ABSTRACT_ASSERT, assertType, subjectType))
                .addMethod(staticAssertThatFactory(assertType, subjectType))
                .addMethod(privateConstructorFor(assertType, subjectType))
                .addMethods(fieldAssertMethods(assertType, fields))
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

    private MethodSpec staticAssertThatFactory(ClassName assertType, ClassName subjectType) {
        return MethodSpec.methodBuilder("assertThat")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(assertType)
                .addParameter(subjectType, "actual")
                .addStatement("return new $T(actual)", assertType)
                .build();
    }

    private MethodSpec privateConstructorFor(ClassName assertType, ClassName subjectType) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(subjectType, "actual")
                .addStatement("super(actual, $T.class)", assertType)
                .build();
    }

    private List<MethodSpec> fieldAssertMethods(ClassName assertType, List<VariableManifest> fields) {
        return fields.stream()
                .map(field -> fieldAssertMethod(assertType, field))
                .toList();
    }

    private MethodSpec fieldAssertMethod(ClassName assertType, VariableManifest field) {
        if (field.type().is(List.class)) {
            return listFieldAssertMethod(assertType, field);
        }
        return equalsFieldAssertMethod(assertType, field);
    }

    private MethodSpec equalsFieldAssertMethod(ClassName assertType, VariableManifest field) {
        var fieldName = field.name();
        var paramType = field.type().asBoxed().asTypeName();
        return MethodSpec.methodBuilder("has" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(assertType)
                .addParameter(paramType, "expected")
                .addStatement("isNotNull()")
                .beginControlFlow("if (!$T.equals(actual.$N(), expected))", Objects.class, fieldName)
                .addStatement("failWithMessage($S, expected, actual.$N())",
                        "Expected " + fieldName + " to be <%s> but was <%s>", fieldName)
                .endControlFlow()
                .addStatement("return this")
                .build();
    }

    private MethodSpec listFieldAssertMethod(ClassName assertType, VariableManifest field) {
        var fieldName = field.name();
        var listElementType = listElementTypeOf(field);
        var listAssertType = ParameterizedTypeName.get(LIST_ASSERT, listElementType);
        var requirementsType = ParameterizedTypeName.get(CONSUMER, listAssertType);
        return MethodSpec.methodBuilder("has" + capitalize(fieldName) + "Satisfying")
                .addModifiers(Modifier.PUBLIC)
                .returns(assertType)
                .addParameter(requirementsType, "requirements")
                .addStatement("isNotNull()")
                .addStatement("$T.requireNonNull(requirements, $S)", Objects.class, "requirements must not be null")
                .addStatement("$T actualListAssert = $T.assertThat(actual.$N())", listAssertType, ASSERTIONS, fieldName)
                .addStatement("requirements.accept(actualListAssert)")
                .addStatement("return this")
                .build();
    }

    private TypeName listElementTypeOf(VariableManifest field) {
        if (field.type().parameters().isEmpty()) {
            return TypeName.get(Object.class);
        }
        return field.type().parameters().getFirst().asBoxed().asTypeName();
    }

    private MethodSpec assertThatDelegateMethod(ClassName subjectType, ClassName assertType) {
        return MethodSpec.methodBuilder("assertThat")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(assertType)
                .addParameter(subjectType, "actual")
                .addStatement("return $T.assertThat(actual)", assertType)
                .build();
    }

    private void addEntry(String packageName, ClassName subjectType, ClassName assertType) {
        entriesByPackage.computeIfAbsent(packageName, k -> new CopyOnWriteArrayList<>())
                .add(new AssertionEntry(subjectType, assertType));
    }

    private boolean isNestedRecordType(TypeManifest type) {
        if (!type.isRecord()) return false;
        if (type.isSingleValueType()) return false;
        if (type.isStandardType()) return false;
        if (type.isCustomType()) return false;
        if (!type.parameters().isEmpty()) return false;
        if (type.is(Instant.class) || type.is(LocalDate.class) || type.is(LocalDateTime.class)
                || type.is(Duration.class)) return false;
        return !type.is(BigDecimal.class);
    }

    record AssertionEntry(ClassName subjectType, ClassName assertType) {
    }
}
