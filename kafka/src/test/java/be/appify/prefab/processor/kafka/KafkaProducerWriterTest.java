package be.appify.prefab.processor.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import be.appify.prefab.processor.PrefabProcessor;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaProducerWriterTest {
    @Test
    void eventWithoutHandlerGeneratesProducer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.single.infrastructure.kafka.UserCreatedKafkaProducer",
                "expected/kafka/single/UserCreatedKafkaProducer.java");
    }

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

    @Test
    void subtypeEventsGenerateOnlySupertypePublisher() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/supertype/UserEvent.java"),
                        sourceOf("kafka/supertype/UserCreated.java"),
                        sourceOf("kafka/supertype/UserUpdated.java"),
                        sourceOf("kafka/supertype/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.supertype.infrastructure.kafka.UserEventKafkaProducer",
                "expected/kafka/supertype/UserEventKafkaProducer.java");
        Assertions.assertThrows(AssertionError.class,
                () -> assertThat(compilation).generatedSourceFile("kafka.supertype.infrastructure.kafka.UserCreatedKafkaProducer"));
        Assertions.assertThrows(AssertionError.class,
                () -> assertThat(compilation).generatedSourceFile("kafka.supertype.infrastructure.kafka.UserUpdatedKafkaProducer"));
    }

    @Test
    void importedDependencyEventGeneratesKafkaProducer() {
        var dependencyClasspath = compileDependencyClasspath(sourceOf("kafka/dependencyevents/ExternalUserCreated.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("kafka/externaldependency/UserImporter.java"));

            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedSourceFile("kafka.dependencyevents.infrastructure.kafka.ExternalUserCreatedKafkaProducer")
                    .isNotNull();
            assertThat(compilation)
                    .generatedSourceFile("kafka.dependencyevents.infrastructure.kafka.ExternalUserCreatedKafkaEventTypeRegistrar")
                    .isNotNull();
        } finally {
            deleteRecursively(dependencyClasspath);
        }
    }

    @Test
    void importedAvscDependencyEventGeneratesKafkaProducer() {
        var dependencyClasspath = compileDependencyClasspath(sourceOf("kafka/dependencyavsc/ExternalOrderCreated.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("kafka/externaldependencyavsc/ExternalOrderImporter.java"));

            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedSourceFile("kafka.dependencyavsc.infrastructure.kafka.ExternalOrderCreatedKafkaProducer")
                    .isNotNull();
            assertThat(compilation)
                    .generatedSourceFile("kafka.dependencyavsc.infrastructure.kafka.ExternalOrderCreatedKafkaEventTypeRegistrar")
                    .isNotNull();
        } finally {
            deleteRecursively(dependencyClasspath);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
