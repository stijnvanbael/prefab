package be.appify.prefab.processor.kafka;

import org.junit.jupiter.api.Test;

import be.appify.prefab.processor.PrefabProcessor;

import java.io.IOException;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaConsumerWriterTest {
    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.single.infrastructure.kafka.UserExporterKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/single/UserExporterKafkaConsumer.java"));
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
        assertThat(compilation).generatedSourceFile("kafka.multiple.infrastructure.kafka.UserExporterKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/multiple/UserExporterKafkaConsumer.java"));
    }

    @Test
    void noParentEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/noparent/User.java"),
                        sourceOf("kafka/noparent/UserEvent.java"),
                        sourceOf("kafka/noparent/UserExporter.java"));
        assertThat(compilation).hadErrorContaining("share the same topic [user] but have no common ancestor");
    }

    @Test
    void multipleTopics() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multitopic/Sale.java"),
                        sourceOf("kafka/multitopic/Refund.java"),
                        sourceOf("kafka/multitopic/DayTotal.java"),
                        sourceOf("kafka/multitopic/DayTotalRepositoryMixin.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.multitopic.infrastructure.kafka.DayTotalKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/multitopic/DayTotalKafkaConsumer.java"));
    }
}
