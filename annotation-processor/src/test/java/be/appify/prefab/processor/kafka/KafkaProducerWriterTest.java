package be.appify.prefab.processor.kafka;

import org.junit.jupiter.api.Test;

import be.appify.prefab.processor.PrefabProcessor;

import java.io.IOException;

import static be.appify.prefab.processor.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaProducerWriterTest {
    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.single.infrastructure.kafka.UserCreatedKafkaProducer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/single/UserCreatedKafkaProducer.java"));
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multiple/User.java"),
                        sourceOf("kafka/multiple/UserEvent.java"),
                        sourceOf("kafka/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.multiple.infrastructure.kafka.UserEventKafkaProducer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/multiple/UserEventKafkaProducer.java"));
    }
}
