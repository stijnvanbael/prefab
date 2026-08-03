package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.FileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabProcessor;
import be.appify.prefab.processor.TestClientWriter;
import com.google.testing.compile.Compilation;
import com.palantir.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.palantir.javapoet.JavaFile.builder;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluralAggregateTest {

    private static final Compilation compilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("rest/plural/source/Goose.java"),
                    sourceOf("rest/plural/source/Gosling.java"),
                    sourceOf("rest/plural/source/Fungus.java"));

    @Test
    void compiles() {
        assertThat(compilation).succeeded();
    }

    @Test
    void controllerPathUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.GooseController")
                .contentsAsUtf8String()
                .contains("path = \"geese\"");
    }

    @Test
    void childAggregatePathUsesParentsCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.GoslingController")
                .contentsAsUtf8String()
                .contains("geese/{gooseId}/goslings");
    }

    @Test
    void polymorphicRootPathUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.FungusController")
                .contentsAsUtf8String()
                .contains("path = \"fungi\"");
    }

    @Test
    void createOrUpdateRedirectPathUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.GooseController")
                .contentsAsUtf8String()
                .contains("/geese/");
    }

    @Test
    void polymorphicCreateRedirectPathUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.FungusController")
                .contentsAsUtf8String()
                .contains("/fungi/");
    }

    @Test
    void getListOperationSummaryUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.GooseController")
                .contentsAsUtf8String()
                .contains("List Geese");
    }

    @Test
    void polymorphicGetListOperationSummaryUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.infrastructure.http.FungusController")
                .contentsAsUtf8String()
                .contains("List Fungi");
    }

    @Test
    void serviceDebugLogUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.application.GooseService")
                .contentsAsUtf8String()
                .contains("Getting Geese by");
    }

    @Test
    void polymorphicServiceDebugLogUsesCustomPlural() {
        assertThat(compilation)
                .generatedSourceFile("rest.plural.application.FungusService")
                .contentsAsUtf8String()
                .contains("Getting Fungi");
    }

    @Test
    void testClientMethodNamesUseCustomPlural() {
        var processor = new CapturingTestClientProcessor();
        var testClientCompilation = javac()
                .withProcessors(processor)
                .compile(
                        sourceOf("rest/plural/source/Goose.java"),
                        sourceOf("rest/plural/source/Gosling.java"),
                        sourceOf("rest/plural/source/Fungus.java"));

        assertThat(testClientCompilation).succeeded();
        var gooseClientSource = sourceContaining(processor.capturedSources, "class GooseClient");
        assertTrue(gooseClientSource.contains("findGeese"),
                "Expected GooseClient to declare findGeese but got:\n" + gooseClientSource);

        var fungusClientSource = sourceContaining(processor.capturedSources, "class FungusClient");
        assertTrue(fungusClientSource.contains("findFungi"),
                "Expected FungusClient to declare findFungi but got:\n" + fungusClientSource);
    }

    private static String sourceContaining(List<String> sources, String needle) {
        return sources.stream()
                .filter(s -> s.contains(needle))
                .findFirst()
                .orElse("");
    }

    @javax.annotation.processing.SupportedAnnotationTypes({"be.appify.prefab.core.annotations.*"})
    static class CapturingTestClientProcessor extends PrefabProcessor {
        final List<String> capturedSources = new ArrayList<>();

        @Override
        protected TestClientWriter createTestClientWriter(PrefabContext context) {
            return new TestClientWriter(context, new CapturingFileOutput(capturedSources));
        }
    }

    static class CapturingFileOutput implements FileOutput {
        private final List<String> capturedSources;

        CapturingFileOutput(List<String> capturedSources) {
            this.capturedSources = capturedSources;
        }

        @Override
        public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
            capturedSources.add(builder(packagePrefix, type).build().toString());
        }
    }
}
