package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Verifies that converters are generated for records nested below nested records (2+ levels deep).
 */
class DeepNestedRecordTest {

    @Test
    void deepNestedRecord_schemaFactories() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/deepnestedrecord/source/DeepNestedRecordEvent.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.DeepNestedRecordEventSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/DeepNestedRecordEventSchemaFactory.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.DeepNestedRecordEventOrderSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderSchemaFactory.java"));
        // 2-levels-deep — the converter that was missing before the fix
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.DeepNestedRecordEventOrderAddressSchemaFactory")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderAddressSchemaFactory.java"));
    }

    @Test
    void deepNestedRecord_toGenericRecordConverters() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/deepnestedrecord/source/DeepNestedRecordEvent.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.DeepNestedRecordEventToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/DeepNestedRecordEventToGenericRecordConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.DeepNestedRecordEventOrderToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderToGenericRecordConverter.java"));
        // 2-levels-deep — the converter that was missing before the fix
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.DeepNestedRecordEventOrderAddressToGenericRecordConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/DeepNestedRecordEventOrderAddressToGenericRecordConverter.java"));
    }

    @Test
    void deepNestedRecord_fromGenericRecordConverters() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/deepnestedrecord/source/DeepNestedRecordEvent.java"));
        assertThat(compilation).succeeded();

        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToDeepNestedRecordEventConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/GenericRecordToDeepNestedRecordEventConverter.java"));
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToDeepNestedRecordEventOrderConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/GenericRecordToDeepNestedRecordEventOrderConverter.java"));
        // 2-levels-deep — the converter that was missing before the fix
        assertThat(compilation).generatedSourceFile("event.avro.infrastructure.avro.GenericRecordToDeepNestedRecordEventOrderAddressConverter")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/avro/deepnestedrecord/expected/GenericRecordToDeepNestedRecordEventOrderAddressConverter.java"));
    }
}
