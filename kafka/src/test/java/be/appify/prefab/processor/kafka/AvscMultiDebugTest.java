package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AvscMultiDebugTest {
    @Test
    void debug() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscmulti/OrderCreated.java"),
                        sourceOf("kafka/avscmulti/OrderShipped.java"),
                        sourceOf("kafka/avscmulti/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        System.out.println("Generated files:");
        compilation.generatedSourceFiles().forEach(f -> {
            System.out.println("  " + f.toUri());
            try { System.out.println(f.getCharContent(true)); } catch (Exception e) { /* ignore */ }
        });
    }
}

