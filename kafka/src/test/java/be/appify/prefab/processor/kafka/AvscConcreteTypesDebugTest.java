package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AvscConcreteTypesDebugTest {
    @Test
    void debug() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscaggregate/OrderEvent.java"),
                        sourceOf("kafka/avscaggregate/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        System.out.println("=== Generated source files ===");
        compilation.generatedSourceFiles().forEach(f -> {
            System.out.println("FILE: " + f.toUri());
            if (f.toUri().toString().contains("OrderProcessorKafkaConsumer")) {
                try { System.out.println(f.getCharContent(true)); } catch (Exception ignored) {}
            }
        });
    }
}

