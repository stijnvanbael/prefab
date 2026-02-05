package be.appify.prefab.processor.event.avro;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventToGenericRecordConverterWriterTest {
    @Test
    void simpleEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.SimpleEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/simple/expected/SimpleEventToGenericRecordConverter.java"));
    }

    @Test
    void inheritedFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.InheritEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/inherited/expected/InheritEventToGenericRecordConverter.java"));
    }

    @Test
    void nonPrimitiveFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NonPrimitiveEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nonprimitive/expected/NonPrimitiveEventToGenericRecordConverter.java"));
    }

    @Test
    void nestedRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NestedRecordEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nestedrecord/expected/NestedRecordEventToGenericRecordConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NestedRecordEventMoneyToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nestedrecord/expected/NestedRecordEventMoneyToGenericRecordConverter.java"));
    }

    @Test
    void arrayField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.ArrayFieldEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/array/expected/ArrayFieldEventToGenericRecordConverter.java"));
    }

    @Test
    void sealedHierarchy() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.HierarchyEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/HierarchyEventToGenericRecordConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.HierarchyEventCreatedToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/HierarchyEventCreatedToGenericRecordConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.HierarchyEventUpdatedToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/HierarchyEventUpdatedToGenericRecordConverter.java"));
    }
}
