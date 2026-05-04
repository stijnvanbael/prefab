package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Verifies that converters are generated for records nested below nested records (2+ levels deep).
 */
class DeepNestedRecordTest {

    @Test
    void deepNestedRecord_schemaFactories() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/deepnestedrecord/source/DeepNestedRecordEvent.java"));
        assertThat(compilation).succeeded();

        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DeepNestedRecordEventSchemaFactory",
                "event/avro/deepnestedrecord/expected/DeepNestedRecordEventSchemaFactory.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DeepNestedRecordEventOrderSchemaFactory",
                "event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderSchemaFactory.java");
        // 2-levels-deep — the converter that was missing before the fix
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DeepNestedRecordEventOrderAddressSchemaFactory",
                "event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderAddressSchemaFactory.java");
    }

    @Test
    void deepNestedRecord_toGenericRecordConverters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/deepnestedrecord/source/DeepNestedRecordEvent.java"));
        assertThat(compilation).succeeded();

        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DeepNestedRecordEventToGenericRecordConverter",
                "event/avro/deepnestedrecord/expected/DeepNestedRecordEventToGenericRecordConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DeepNestedRecordEventOrderToGenericRecordConverter",
                "event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderToGenericRecordConverter.java");
        // 2-levels-deep — the converter that was missing before the fix
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.DeepNestedRecordEventOrderAddressToGenericRecordConverter",
                "event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderAddressToGenericRecordConverter.java");
    }

    @Test
    void deepNestedRecord_fromGenericRecordConverters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/deepnestedrecord/source/DeepNestedRecordEvent.java"));
        assertThat(compilation).succeeded();

        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToDeepNestedRecordEventConverter",
                "event/avro/deepnestedrecord/expected/GenericRecordToDeepNestedRecordEventConverter.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToDeepNestedRecordEventOrderConverter",
                "event/avro/deepnestedrecord/expected/GenericRecordToDeepNestedRecordEventOrderConverter.java");
        // 2-levels-deep — the converter that was missing before the fix
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "event.avro.infrastructure.avro.GenericRecordToDeepNestedRecordEventOrderAddressConverter",
                "event/avro/deepnestedrecord/expected/GenericRecordToDeepNestedRecordEventOrderAddressConverter.java");
    }
}
