package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaEventTypeRegistrarWriterTest {

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
                "kafka.single.infrastructure.event.UserCreatedEventTypeRegistrar",
                "expected/kafka/single/UserCreatedEventTypeRegistrar.java");
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
                "kafka.multiple.infrastructure.event.UserEventEventTypeRegistrar",
                "expected/kafka/multiple/UserEventEventTypeRegistrar.java");
    }

    @Test
    void multipleTopics() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multitopic/Sale.java"),
                        sourceOf("kafka/multitopic/Refund.java"),
                        sourceOf("kafka/multitopic/DayTotal.java"),
                        sourceOf("kafka/multitopic/DayTotalRepositoryMixin.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.multitopic.infrastructure.event.SaleCreatedEventTypeRegistrar",
                "expected/kafka/multitopic/SaleCreatedEventTypeRegistrar.java");
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.multitopic.infrastructure.event.RefundCreatedEventTypeRegistrar",
                "expected/kafka/multitopic/RefundCreatedEventTypeRegistrar.java");
    }

    @Test
    void multipleTopicsPerEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multitopicevent/UserEvent.java"),
                        sourceOf("kafka/multitopicevent/UserService.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.multitopicevent.infrastructure.event.UserEventEventTypeRegistrar",
                "expected/kafka/multitopicevent/UserEventEventTypeRegistrar.java");
    }

    @Test
    void createOrUpdateHandler() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/createorupdate/ChannelSummary.java"),
                        sourceOf("kafka/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.createorupdate.infrastructure.event.MessageEventEventTypeRegistrar",
                "expected/kafka/createorupdate/MessageEventEventTypeRegistrar.java");
    }

    @Test
    void avscEventRegistrar() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avsc/OrderCreated.java"),
                        sourceOf("kafka/avsc/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "kafka.avsc.infrastructure.event.OrderCreatedEventEventTypeRegistrar",
                "expected/kafka/avsc/OrderCreatedEventEventTypeRegistrar.java");
    }

    @Test
    void dependencyEventDoesNotGenerateRegistrarInConsumer() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("kafka/dependencyevents/ExternalUserCreated.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("kafka/externaldependency/UserImporter.java"));
            assertThat(compilation).succeeded();
            // The registrar for ExternalUserCreated is generated when the dependency module is compiled,
            // not in the consumer module — so no registrar should appear in this compilation.
            var noRegistrar = compilation.generatedSourceFiles().stream()
                    .noneMatch(f -> f.toUri().getPath().contains("ExternalUserCreatedEventTypeRegistrar"));
            org.junit.jupiter.api.Assertions.assertTrue(noRegistrar);
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

