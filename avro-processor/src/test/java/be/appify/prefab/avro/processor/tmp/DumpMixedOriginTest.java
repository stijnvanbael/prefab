package be.appify.prefab.avro.processor.tmp;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DumpMixedOriginTest {
    @Test
    void dump() throws Exception {
        Compilation compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/avro/mixedorigin/source/MixedOriginAvsc.java"),
                        sourceOf("event/avro/mixedorigin/source/MixedOriginOuterEvent.java"));
        assertThat(compilation).succeeded();
        for (var file : compilation.generatedSourceFiles()) {
            if (file.getName().contains("GenericRecordToMixedOriginOuterEventConverter")) {
                Files.writeString(Path.of("C:/Users/QTJ487/mixed_origin_dump.java"), file.getCharContent(true).toString());
            }
        }
    }
}
