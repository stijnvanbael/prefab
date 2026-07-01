package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

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
        var source = generatedSourceOf(compilation, "kafka.single.infrastructure.event.UserCreatedEventTypeRegistrar");
        assertThat(source).contains("@Component(\"kafka_single_UserCreatedEventTypeRegistrar\")");
        assertThat(source).contains("implements EventRegistryCustomizer");
        assertThat(source).contains("registry.register(\"prefab.user\", UserCreated.class, Event.Serialization.JSON");
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
        var source = generatedSourceOf(compilation, "kafka.multiple.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"kafka_multiple_UserEventEventTypeRegistrar\")");
        assertThat(source).contains("implements EventRegistryCustomizer");
        // topic injected via @Value, registered dynamically
        assertThat(source).contains("@Value(\"${topic.user.name}\")");
        assertThat(source).contains("registry.register(userEventTopic, UserEvent.class, Event.Serialization.JSON");
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

        var saleSrc = generatedSourceOf(compilation, "kafka.multitopic.infrastructure.event.SaleCreatedEventTypeRegistrar");
        assertThat(saleSrc).contains("@Component(\"kafka_multitopic_SaleCreatedEventTypeRegistrar\")");
        assertThat(saleSrc).contains("registry.register(saleCreatedTopic, Sale.Created.class, Event.Serialization.JSON)");

        var refundSrc = generatedSourceOf(compilation, "kafka.multitopic.infrastructure.event.RefundCreatedEventTypeRegistrar");
        assertThat(refundSrc).contains("@Component(\"kafka_multitopic_RefundCreatedEventTypeRegistrar\")");
        assertThat(refundSrc).contains("registry.register(refundCreatedTopic, Refund.Created.class, Event.Serialization.JSON)");
    }

    @Test
    void multipleTopicsPerEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multitopicevent/UserEvent.java"),
                        sourceOf("kafka/multitopicevent/UserService.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.multitopicevent.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"kafka_multitopicevent_UserEventEventTypeRegistrar\")");
        // two topic fields injected for the two topics
        assertThat(source).contains("@Value(\"${topic.user.primary}\")");
        assertThat(source).contains("@Value(\"${topic.user.secondary}\")");
        assertThat(source).contains("registry.register(userEventTopic0, UserEvent.class, Event.Serialization.JSON)");
        assertThat(source).contains("registry.register(userEventTopic1, UserEvent.class, Event.Serialization.JSON)");
    }

    @Test
    void publishToAll() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/publishtoall/UserEvent.java"),
                        sourceOf("kafka/publishtoall/UserService.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.publishtoall.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"kafka_publishtoall_UserEventEventTypeRegistrar\")");
        assertThat(source).contains("registry.register(");
        // publishToAll must register the PublishTo.ALL behaviour
        assertThat(source).contains("registry.registerPublishTo(UserEvent.class, PublishTo.ALL)");
    }

    @Test
    void createOrUpdateHandler() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/createorupdate/ChannelSummary.java"),
                        sourceOf("kafka/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.createorupdate.infrastructure.event.MessageEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"kafka_createorupdate_MessageEventEventTypeRegistrar\")");
        assertThat(source).contains("registry.register(messageEventTopic, MessageEvent.class, Event.Serialization.JSON)");
    }

    @Test
    void avscEventRegistrar() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avsc/OrderCreated.java"),
                        sourceOf("kafka/avsc/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.avsc.infrastructure.event.OrderCreatedEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"kafka_avsc_OrderCreatedEventEventTypeRegistrar\")");
        // AVRO serialization must be used for AVSC events
        assertThat(source).contains("registry.register(\"prefab.order\", OrderCreatedEvent.class, Event.Serialization.AVRO)");
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
