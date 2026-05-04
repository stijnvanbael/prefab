package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventToGenericRecordConverterWriterTest {
    @Test
    void simpleEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.SimpleEventToGenericRecordConverter",
                "event/avro/simple/expected/SimpleEventToGenericRecordConverter.java");
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
                "event.avro.infrastructure.avro.InheritEventToGenericRecordConverter",
                "event/avro/inherited/expected/InheritEventToGenericRecordConverter.java");
    }

    @Test
    void nonPrimitiveFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NonPrimitiveEventToGenericRecordConverter",
                "event/avro/nonprimitive/expected/NonPrimitiveEventToGenericRecordConverter.java");
    }

    @Test
    void nullableFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NullableEventToGenericRecordConverter",
                "event/avro/nullable/expected/NullableEventToGenericRecordConverter.java");
    }

    @Test
    void nestedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NestedRecordEventToGenericRecordConverter",
                "event/avro/nestedrecord/expected/NestedRecordEventToGenericRecordConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.NestedRecordEventMoneyToGenericRecordConverter",
                "event/avro/nestedrecord/expected/NestedRecordEventMoneyToGenericRecordConverter.java");
    }

    @Test
    void arrayField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.ArrayFieldEventToGenericRecordConverter",
                "event/avro/array/expected/ArrayFieldEventToGenericRecordConverter.java");
    }

    @Test
    void sealedHierarchy() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.HierarchyEventToGenericRecordConverter",
                "event/avro/hierarchy/expected/HierarchyEventToGenericRecordConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.HierarchyEventCreatedToGenericRecordConverter",
                "event/avro/hierarchy/expected/HierarchyEventCreatedToGenericRecordConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.HierarchyEventUpdatedToGenericRecordConverter",
                "event/avro/hierarchy/expected/HierarchyEventUpdatedToGenericRecordConverter.java");
    }
}
