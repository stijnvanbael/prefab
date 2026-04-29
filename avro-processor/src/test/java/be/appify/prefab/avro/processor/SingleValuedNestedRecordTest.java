package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Verifies that a single-value record whose only component is itself a record is treated as a
 * nested record (not as a logical string-wrapper type), so the correct nested-record converter
 * is generated rather than an incorrect {@code toString()} call.
 */
class SingleValuedNestedRecordTest {

    @Test
    void singleValuedNested_schemaFactories() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/singlevaluednested/source/SingleValuedNestedEvent.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.SingleValuedNestedEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/singlevaluednested/expected/SingleValuedNestedEventSchemaFactory.java"));
        // Outer is a single-value type whose component is Inner (a record) — must get its own schema factory
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.SingleValuedNestedEventOuterSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/singlevaluednested/expected/SingleValuedNestedEventOuterSchemaFactory.java"));
    }

    @Test
    void singleValuedNested_toGenericRecordConverters() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/singlevaluednested/source/SingleValuedNestedEvent.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.SingleValuedNestedEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/singlevaluednested/expected/SingleValuedNestedEventToGenericRecordConverter.java"));
        // Outer must produce a proper nested-record converter, not one using toString()
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.SingleValuedNestedEventOuterToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/singlevaluednested/expected/SingleValuedNestedEventOuterToGenericRecordConverter.java"));
    }

    @Test
    void singleValuedNested_fromGenericRecordConverters() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/singlevaluednested/source/SingleValuedNestedEvent.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToSingleValuedNestedEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/singlevaluednested/expected/GenericRecordToSingleValuedNestedEventConverter.java"));
        // Outer must produce a proper nested-record converter, not one using toString()
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToSingleValuedNestedEventOuterConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/singlevaluednested/expected/GenericRecordToSingleValuedNestedEventOuterConverter.java"));
    }
}

