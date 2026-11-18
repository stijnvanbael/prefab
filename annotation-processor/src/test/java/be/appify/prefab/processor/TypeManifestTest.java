package be.appify.prefab.processor;

import com.google.testing.compile.JavaFileObjects;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.google.common.truth.Truth.assertThat;

class TypeManifestTest {

    @Test
    void wildcardArgumentsPreserveBoundsAndIdentity() {
        var processor = new CapturingProcessor();
        var compilation = javac().withProcessors(processor).compile(
                JavaFileObjects.forSourceString("example.Wildcards", """
                        package example;
                        import java.util.List;
                        class Wildcards {
                            List<?> unbounded;
                            List<? extends Number> upper;
                            List<? super String> lower;
                            List<? extends List<? super String>> nested;
                            Builder<?> builder;
                            static class Builder<SELF extends Builder<SELF>> {}
                        }
                        """));
        assertThat(compilation).succeeded();
        assertThat(processor.types.get("unbounded").asTypeName().toString()).isEqualTo("java.util.List<?>");
        assertThat(processor.types.get("upper").asTypeName().toString())
                .isEqualTo("java.util.List<? extends java.lang.Number>");
        assertThat(processor.types.get("lower").asTypeName().toString())
                .isEqualTo("java.util.List<? super java.lang.String>");
        assertThat(processor.types.get("nested").asTypeName().toString())
                .isEqualTo("java.util.List<? extends java.util.List<? super java.lang.String>>");
        assertThat(processor.types.get("builder").asTypeName().toString())
                .isEqualTo("example.Wildcards.Builder<?>");
        assertThat(processor.types.get("upper")).isNotEqualTo(processor.types.get("lower"));
        assertThat(processor.types.get("unbounded").parameters().getFirst().asElement()).isNull();
        assertThat(processor.types.get("unbounded").parameters().getFirst().fields()).isEmpty();
    }

    @Test
    void nullableTypeUseIsRecognizedOnDeclarationsAndRecordAccessors() {
        var processor = new CapturingProcessor();
        var compilation = javac().withProcessors(processor).compile(
                JavaFileObjects.forSourceString("example.Nullability", """
                        package example;
                        import java.util.List;
                        import org.jspecify.annotations.Nullable;

                        class Nullability {
                            @Nullable String nullableField;
                            List<@Nullable String> nullableElement;
                            @Nullable List<String> nullableContainer;
                            String @Nullable [] nullableArray;
                            @Nullable String[] nullableArrayElement;

                            void method(@Nullable String nullableParameter) {}

                            @Nullable String nullableReturn() { return null; }
                        }

                        record AccessorRecord(String value) {
                            @Nullable public String value() { return value; }
                        }
                        """));

        assertThat(compilation).succeeded();
        assertThat(processor.variables.get("nullableField").nullable()).isTrue();
        assertThat(processor.variables.get("nullableElement").nullable()).isFalse();
        assertThat(processor.variables.get("nullableContainer").nullable()).isTrue();
        assertThat(processor.variables.get("nullableArray").nullable()).isTrue();
        assertThat(processor.variables.get("nullableArrayElement").nullable()).isFalse();
        assertThat(processor.variables.get("nullableParameter").nullable()).isTrue();
        assertThat(processor.methodReturns.get("nullableReturn").nullable()).isTrue();
        assertThat(processor.recordFields.get("AccessorRecord").getFirst().nullable()).isTrue();
    }

    @SupportedAnnotationTypes("*")
    static class CapturingProcessor extends AbstractProcessor {
        final Map<String, TypeManifest> types = new LinkedHashMap<>();
        final Map<String, VariableManifest> variables = new LinkedHashMap<>();
        final Map<String, VariableManifest> methodReturns = new LinkedHashMap<>();
        final Map<String, List<VariableManifest>> recordFields = new LinkedHashMap<>();

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
            roundEnvironment.getRootElements().stream()
                    .filter(TypeElement.class::isInstance)
                    .map(TypeElement.class::cast)
                    .forEach(type -> {
                        ElementFilter.fieldsIn(type.getEnclosedElements()).forEach(this::capture);
                        ElementFilter.methodsIn(type.getEnclosedElements()).forEach(method -> {
                            method.getParameters().forEach(this::capture);
                            if (method.getReturnType().getKind() != TypeKind.VOID) {
                                methodReturns.put(method.getSimpleName().toString(),
                                        VariableManifest.ofMethod(method, processingEnv));
                            }
                        });
                        if (!type.getRecordComponents().isEmpty()) {
                            recordFields.put(type.getSimpleName().toString(),
                                    TypeManifest.of(type.asType(), processingEnv).fields());
                        }
                    });
            return false;
        }

        private void capture(VariableElement field) {
            types.put(field.getSimpleName().toString(), TypeManifest.of(field.asType(), processingEnv));
            variables.put(field.getSimpleName().toString(), VariableManifest.of(field, processingEnv));
        }
    }
}
