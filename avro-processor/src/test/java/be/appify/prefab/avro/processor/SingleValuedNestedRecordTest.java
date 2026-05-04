package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Verifies that a single-value record whose only component is itself a record is treated as a
 * nested record (not as a logical string-wrapper type), so the correct nested-record converter
 * is generated rather than an incorrect {@code toString()} call.
 */
class SingleValuedNestedRecordTest {

    @Test
    void singleValuedNested_schemaFactories() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/singlevaluednested/source/SingleValuedNestedEvent.java"));
        assertThat(compilation).succeeded();

        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.SingleValuedNestedEventSchemaFactory",
                "event/avro/singlevaluednested/expected/SingleValuedNestedEventSchemaFactory.java");
        // Outer is a single-value type whose component is Inner (a record) — must get its own schema factory
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.SingleValuedNestedEventOuterSchemaFactory",
                "event/avro/singlevaluednested/expected/SingleValuedNestedEventOuterSchemaFactory.java");
    }

    @Test
    void singleValuedNested_toGenericRecordConverters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/singlevaluednested/source/SingleValuedNestedEvent.java"));
        assertThat(compilation).succeeded();

        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.SingleValuedNestedEventToGenericRecordConverter",
                "event/avro/singlevaluednested/expected/SingleValuedNestedEventToGenericRecordConverter.java");
        // Outer must produce a proper nested-record converter, not one using toString()
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.SingleValuedNestedEventOuterToGenericRecordConverter",
                "event/avro/singlevaluednested/expected/SingleValuedNestedEventOuterToGenericRecordConverter.java");
    }

    @Test
    void singleValuedNested_fromGenericRecordConverters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/singlevaluednested/source/SingleValuedNestedEvent.java"));
        assertThat(compilation).succeeded();

        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToSingleValuedNestedEventConverter",
                "event/avro/singlevaluednested/expected/GenericRecordToSingleValuedNestedEventConverter.java");
        // Outer must produce a proper nested-record converter, not one using toString()
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToSingleValuedNestedEventOuterConverter",
                "event/avro/singlevaluednested/expected/GenericRecordToSingleValuedNestedEventOuterConverter.java");
    }
}

