package be.appify.prefab.processor.persistence;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class PolymorphicConverterWriterTest {

    @Test
    void polymorphicAggregateGeneratesReadingConverter() throws IOException {
        var source = sourceOf("persistence/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("persistence.polymorphic.infrastructure.persistence.ShapeReadingConverter")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateGeneratesRepository() throws IOException {
        var source = sourceOf("persistence/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("persistence.polymorphic.application.ShapeRepository")
                .isNotNull();
    }

    @Test
    void polymorphicAggregateReadingConverterContent() throws IOException {
        var source = sourceOf("persistence/polymorphic/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("persistence.polymorphic.infrastructure.persistence.ShapeReadingConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("persistence/polymorphic/expected/ShapeReadingConverter.java"));
    }

    private static String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static javax.tools.JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return com.google.testing.compile.JavaFileObjects.forResource(resource);
    }
}
