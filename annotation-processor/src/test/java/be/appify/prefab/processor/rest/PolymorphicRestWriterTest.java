package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class PolymorphicRestWriterTest {

    @Test
    void polymorphicAggregateGeneratesController() throws IOException {
        var source = sourceOf("rest/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeController")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateGeneratesResponseType() throws IOException {
        var source = sourceOf("rest/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeResponse")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateGeneratesService() throws IOException {
        var source = sourceOf("rest/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.polymorphic.application.ShapeService")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateControllerContent() throws IOException {
        var source = sourceOf("rest/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeController")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/ShapeController.java"));
    }

    @Test
    void polymorphicAggregateResponseTypeContent() throws IOException {
        var source = sourceOf("rest/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.polymorphic.infrastructure.http.ShapeResponse")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/ShapeResponse.java"));
    }

    @Test
    void polymorphicAggregateServiceContent() throws IOException {
        var source = sourceOf("rest/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.polymorphic.application.ShapeService")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("rest/polymorphic/expected/ShapeService.java"));
    }

    private static String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static javax.tools.JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return com.google.testing.compile.JavaFileObjects.forResource(resource);
    }
}
