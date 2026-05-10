package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class GenericRecordToEventConverterWriterTest {
    @Test
    void simpleEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToSimpleEventConverter",
                "event/avro/simple/expected/GenericRecordToSimpleEventConverter.java");
    }

    @Test
    void inheritedFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToInheritEventConverter",
                "event/avro/inherited/expected/GenericRecordToInheritEventConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToSuperTypeConverter",
                "event/avro/inherited/expected/GenericRecordToSuperTypeConverter.java");
    }

    @Test
    void interfaceSupertype() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/interfacesupertype/source/UserEvent.java"),
                        sourceOf("event/avro/interfacesupertype/source/UserCreated.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToUserEventConverter",
                "event/avro/interfacesupertype/expected/GenericRecordToUserEventConverter.java");
    }

    @Test
    void nonPrimitiveFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToNonPrimitiveEventConverter",
                "event/avro/nonprimitive/expected/GenericRecordToNonPrimitiveEventConverter.java");
    }

    @Test
    void nullableFields() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToNullableEventConverter",
                "event/avro/nullable/expected/GenericRecordToNullableEventConverter.java");
    }

    @Test
    void nestedRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToNestedRecordEventConverter",
                "event/avro/nestedrecord/expected/GenericRecordToNestedRecordEventConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToNestedRecordEventMoneyConverter",
                "event/avro/nestedrecord/expected/GenericRecordToNestedRecordEventMoneyConverter.java");
    }

    @Test
    void arrayField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToArrayFieldEventConverter",
                "event/avro/array/expected/GenericRecordToArrayFieldEventConverter.java");
    }

    @Test
    void sealedHierarchy() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToHierarchyEventConverter",
                "event/avro/hierarchy/expected/GenericRecordToHierarchyEventConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToHierarchyEventCreatedConverter",
                "event/avro/hierarchy/expected/GenericRecordToHierarchyEventCreatedConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToHierarchyEventUpdatedConverter",
                "event/avro/hierarchy/expected/GenericRecordToHierarchyEventUpdatedConverter.java");
    }
}
