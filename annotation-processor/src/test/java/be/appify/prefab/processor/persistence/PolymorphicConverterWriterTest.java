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

    @Test
    void embeddedMultiFieldRecordIsConstructedFromSubColumns() throws IOException {
        var source = sourceOf("persistence/polymorphic_with_embedded_record/source/Quiz.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "persistence.polymorphic_with_embedded_record.infrastructure.persistence.QuizReadingConverter")
                .contentsAsUtf8String()
                .contains("new Quiz.Score(conversionService.convert(row.get(\"score\"), Double.class), "
                        + "conversionService.convert(row.get(\"score_max\"), Double.class))");
    }

    @Test
    void instantFieldInEmbeddedRecordUsesConversionService() throws IOException {
        var source = sourceOf("persistence/polymorphic_with_embedded_record/source/Quiz.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "persistence.polymorphic_with_embedded_record.infrastructure.persistence.QuizReadingConverter")
                .contentsAsUtf8String()
                .contains("conversionService.convert(row.get(\"time_span_start\"), Instant.class)");
    }

    @Test
    void nullableInstantFieldInEmbeddedRecordUsesConversionService() throws IOException {
        var source = sourceOf("persistence/polymorphic_with_embedded_record/source/Quiz.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "persistence.polymorphic_with_embedded_record.infrastructure.persistence.QuizReadingConverter")
                .contentsAsUtf8String()
                .contains("conversionService.convert(row.get(\"time_span_end\"), Instant.class)");
    }

    @Test
    void listFieldUsesRawCastInsteadOfConversionService() throws IOException {
        var source = sourceOf("persistence/polymorphic_with_embedded_record/source/Quiz.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "persistence.polymorphic_with_embedded_record.infrastructure.persistence.QuizReadingConverter")
                .contentsAsUtf8String()
                .contains("(List<Quiz.Question>) row.get(\"questions\")");
    }

    @Test
    void interfaceMethodAnnotationsAreInheritedBySubtypeFields() throws IOException {
        var source = sourceOf("persistence/polymorphic_interface_annotations/source/Shape.java");
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "persistence.polymorphic_interface_annotations.infrastructure.persistence.ShapeReadingConverter")
                .isNotNull();
    }

    private static String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static javax.tools.JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return com.google.testing.compile.JavaFileObjects.forResource(resource);
    }
}
