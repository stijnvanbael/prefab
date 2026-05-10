package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventSchemaFactoryWriterTest {
    @Test
    void simpleEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.SimpleEventSchemaFactory",
                "event/avro/simple/expected/SimpleEventSchemaFactory.java");

    }

    @Test
    void inheritedFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.InheritEventSchemaFactory",
                "event/avro/inherited/expected/InheritEventSchemaFactory.java");
    }

    @Test
    void nonPrimitiveFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NonPrimitiveEventSchemaFactory",
                "event/avro/nonprimitive/expected/NonPrimitiveEventSchemaFactory.java");
    }

    @Test
    void nullableFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NullableEventSchemaFactory",
                "event/avro/nullable/expected/NullableEventSchemaFactory.java");
    }

    @Test
    void nestedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NestedRecordEventSchemaFactory",
                "event/avro/nestedrecord/expected/NestedRecordEventSchemaFactory.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NestedRecordEventMoneySchemaFactory",
                "event/avro/nestedrecord/expected/NestedRecordEventMoneySchemaFactory.java");
    }

    @Test
    void arrayField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.ArrayFieldEventSchemaFactory",
                "event/avro/array/expected/ArrayFieldEventSchemaFactory.java");
    }

    @Test
    void sealedHierarchy() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.HierarchyEventSchemaFactory",
                "event/avro/hierarchy/expected/HierarchyEventSchemaFactory.java");
    }

    @Test
    void exampleField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/examplefield/source/ExampleFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.ExampleFieldEventSchemaFactory",
                "event/avro/examplefield/expected/ExampleFieldEventSchemaFactory.java");
    }

    @Test
    void docField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/docfield/source/DocFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DocFieldEventSchemaFactory",
                "event/avro/docfield/expected/DocFieldEventSchemaFactory.java");
    }

    @Test
    void nullableMetadataUsesLatestCompilationSnapshot() {
        var baseSource = JavaFileObjects.forSourceString("event.avro.NullableEvent", """
                package event.avro;

                import be.appify.prefab.core.annotations.Event;

                @Event(topic = "nullable", serialization = Event.Serialization.AVRO)
                public record NullableEvent(
                        String id,
                        String name,
                        String description
                ) {
                }
                """);
        var nullableSource = JavaFileObjects.forSourceString("event.avro.NullableEvent", """
                package event.avro;

                import be.appify.prefab.core.annotations.Event;
                import jakarta.annotation.Nullable;

                @Event(topic = "nullable", serialization = Event.Serialization.AVRO)
                public record NullableEvent(
                        String id,
                        String name,
                        @Nullable String description
                ) {
                }
                """);

        var firstCompilation = javac().withProcessors(new PrefabProcessor()).compile(baseSource);
        assertThat(firstCompilation).succeeded();
        org.assertj.core.api.Assertions.assertThat(
                        generatedSource(firstCompilation, "event.avro.infrastructure.avro.NullableEventSchemaFactory"))
                .doesNotContain("SchemaSupport.createNullableSchema");

        var secondCompilation = javac().withProcessors(new PrefabProcessor()).compile(nullableSource);
        assertThat(secondCompilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                secondCompilation,
                "event.avro.infrastructure.avro.NullableEventSchemaFactory",
                "event/avro/nullable/expected/NullableEventSchemaFactory.java");
    }

    private static String generatedSource(com.google.testing.compile.Compilation compilation, String generatedTypeName) {
        var expectedSuffix = "/" + generatedTypeName.replace('.', '/') + ".java";
        var generatedFile = compilation.generatedSourceFiles().stream()
                .filter(file -> file.toUri().getPath().endsWith(expectedSuffix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Generated source file not found: " + generatedTypeName));
        try {
            return generatedFile.getCharContent(true).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
