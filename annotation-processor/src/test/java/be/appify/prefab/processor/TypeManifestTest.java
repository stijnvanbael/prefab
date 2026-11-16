package be.appify.prefab.processor;

import com.google.testing.compile.JavaFileObjects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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

    @SupportedAnnotationTypes("*")
    static class CapturingProcessor extends AbstractProcessor {
        final Map<String, TypeManifest> types = new LinkedHashMap<>();

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
            roundEnvironment.getRootElements().stream()
                    .flatMap(element -> ElementFilter.fieldsIn(element.getEnclosedElements()).stream())
                    .forEach(this::capture);
            return false;
        }

        private void capture(VariableElement field) {
            types.put(field.getSimpleName().toString(), TypeManifest.of(field.asType(), processingEnv));
        }
    }
}
