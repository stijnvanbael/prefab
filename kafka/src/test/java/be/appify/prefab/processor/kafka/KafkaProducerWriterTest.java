package be.appify.prefab.processor.kafka;

import org.junit.jupiter.api.Test;

import be.appify.prefab.processor.PrefabProcessor;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaProducerWriterTest {
    @Test
    void singleEventType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.single.infrastructure.kafka.UserCreatedKafkaProducer",
                "expected/kafka/single/UserCreatedKafkaProducer.java");
    }

    @Test
    void multipleEventTypes() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multiple/User.java"),
                        sourceOf("kafka/multiple/UserEvent.java"),
                        sourceOf("kafka/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.multiple.infrastructure.kafka.UserEventKafkaProducer",
                "expected/kafka/multiple/UserEventKafkaProducer.java");
    }

    @Test
    void avscEventProducer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avsc/OrderCreated.java"),
                        sourceOf("kafka/avsc/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.avsc.infrastructure.kafka.OrderCreatedEventKafkaProducer",
                "expected/kafka/avsc/OrderCreatedEventKafkaProducer.java");
    }

    @Test
    void avscAggregateEventProducers() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("kafka/avscaggregate/OrderEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.avscaggregate.infrastructure.kafka.OrderCreatedEventKafkaProducer",
                "expected/kafka/avscaggregate/OrderCreatedEventKafkaProducer.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.avscaggregate.infrastructure.kafka.OrderShippedEventKafkaProducer",
                "expected/kafka/avscaggregate/OrderShippedEventKafkaProducer.java");
    }
}
