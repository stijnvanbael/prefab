package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipleAsyncCreateTestClientTest {

    @Test
    void multipleAsyncCreateFactoriesGenerateBothMethodsInTestClient() {
        var processor = new CapturingPrefabProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(sourceOf("rest/asyncmultiplecreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertTrue(processor.capturedSources.stream().anyMatch(s -> s.contains("void placeOrder(")),
                "Expected test client to contain placeOrder method");
        assertTrue(processor.capturedSources.stream().anyMatch(s -> s.contains("void quickOrder(")),
                "Expected test client to contain quickOrder method");
    }

    @Test
    void singleAsyncCreateFactoryGeneratesMethodInTestClient() {
        var processor = new CapturingPrefabProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(sourceOf("rest/asynccreate/source/Order.java"));

        assertThat(compilation).succeeded();
        assertTrue(processor.capturedSources.stream().anyMatch(s -> s.contains("void placeOrder(")),
                "Expected test client to contain placeOrder method");
    }

    static class CapturingPrefabProcessor extends PrefabProcessor {
        final List<String> capturedSources = new ArrayList<>();

        @Override
        protected TestClientWriter createTestClientWriter(PrefabContext context) {
            return new TestClientWriter(context, new CapturingFileWriter(capturedSources));
        }
    }

    static class CapturingFileWriter implements TestFileOutput {
        private final List<String> capturedSources;

        CapturingFileWriter(List<String> capturedSources) {
            this.capturedSources = capturedSources;
        }

        @Override
        public void setPreferredElement(TypeElement element) {
            // no-op: path resolution is not needed when capturing to memory
        }

        @Override
        public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
            capturedSources.add(JavaFile.builder(packagePrefix, type).build().toString());
        }
    }
}
