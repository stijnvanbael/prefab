package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AvscAggregateDebugTest {
    @Test
    void debug() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscaggregate/OrderEvent.java"),
                        sourceOf("kafka/avscaggregate/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        compilation.generatedSourceFiles().forEach(f -> {
            if (f.toUri().toString().contains("OrderProcessorKafkaConsumer")) {
                System.out.println("=== " + f.toUri() + " ===");
                try { System.out.print(f.getCharContent(true)); } catch (Exception ignored) {}
                System.out.println("\n=== END ===");
            }
        });
    }
}

