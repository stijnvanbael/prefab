package be.appify.prefab.processor.event.avro;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventSchemaFactoryWriterTest {
    @Test
    void simpleEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/simple/source/SimpleEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.SimpleEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/simple/expected/SimpleEventSchemaFactory.java"));
    }

    @Test
    void inheritedFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/inherited/source/SuperType.java"),
                        sourceOf("event/avro/inherited/source/InheritEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.InheritEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/inherited/expected/InheritEventSchemaFactory.java"));
    }

    @Test
    void nonPrimitiveFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nonprimitive/source/NonPrimitiveEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NonPrimitiveEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nonprimitive/expected/NonPrimitiveEventSchemaFactory.java"));
    }

    @Test
    void nullableFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nullable/source/NullableEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NullableEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nullable/expected/NullableEventSchemaFactory.java"));
    }

    @Test
    void nestedRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/nestedrecord/source/NestedRecordEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NestedRecordEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nestedrecord/expected/NestedRecordEventSchemaFactory.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.NestedRecordEventMoneySchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/nestedrecord/expected/NestedRecordEventMoneySchemaFactory.java"));
    }

    @Test
    void arrayField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/array/source/ArrayFieldEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.ArrayFieldEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/array/expected/ArrayFieldEventSchemaFactory.java"));
    }

    @Test
    void sealedHierarchy() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/hierarchy/source/HierarchyEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.HierarchyEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/hierarchy/expected/HierarchyEventSchemaFactory.java"));
    }
}
