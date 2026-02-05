package be.appify.prefab.processor.event.avro;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class GenericRecordToEventConverterWriterTest {
    @Test
    void simpleEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToSimpleEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/simple/expected/GenericRecordToSimpleEventConverter.java"));
    }

    @Test
    void inheritedFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToInheritEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/inherited/expected/GenericRecordToInheritEventConverter.java"));
    }

    @Test
    void nonPrimitiveFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToNonPrimitiveEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nonprimitive/expected/GenericRecordToNonPrimitiveEventConverter.java"));
    }

    @Test
    void nestedRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToNestedRecordEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nestedrecord/expected/GenericRecordToNestedRecordEventConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToNestedRecordEventMoneyConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nestedrecord/expected/GenericRecordToNestedRecordEventMoneyConverter.java"));
    }

    @Test
    void arrayField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToArrayFieldEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/array/expected/GenericRecordToArrayFieldEventConverter.java"));
    }

    @Test
    void sealedHierarchy() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToHierarchyEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/GenericRecordToHierarchyEventConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToHierarchyEventCreatedConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/GenericRecordToHierarchyEventCreatedConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToHierarchyEventUpdatedConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/GenericRecordToHierarchyEventUpdatedConverter.java"));
    }
}
