package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaConsumerConfigWriterTest {
    @Test
    void customConcurrency() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        Assertions.assertThrows(AssertionError.class,
                () -> assertThat(compilation).generatedSourceFile("kafka.customdlt.infrastructure.kafka.UserExporterKafkaConsumerConfig"));
    }

    @Test
    void customDeadLetterTopic() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/customdlt/User.java"),
                        sourceOf("kafka/customdlt/UserEvent.java"),
                        sourceOf("kafka/customdlt/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.customdlt.infrastructure.kafka.UserExporterKafkaConsumerConfig",
                "expected/kafka/customdlt/UserExporterKafkaConsumerConfig.java");
    }

    @Test
    void deadLetterTopicDisabled() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/dltdisabled/User.java"),
                        sourceOf("kafka/dltdisabled/UserEvent.java"),
                        sourceOf("kafka/dltdisabled/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.dltdisabled.infrastructure.kafka.UserExporterKafkaConsumerConfig",
                "expected/kafka/dltdisabled/UserExporterKafkaConsumerConfig.java");
    }
}
