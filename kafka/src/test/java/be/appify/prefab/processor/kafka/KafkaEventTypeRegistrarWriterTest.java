package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
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
        assertThat(source).contains("@Value(\"${topic.user.name}\") String[] userEventTopics");
        assertThat(source).contains("for (var topic : userEventTopics)");
        assertThat(source).contains("registry.register(topic, UserEvent.class, Event.Serialization.JSON");
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
        assertThat(saleSrc).contains("registry.register(topic, Sale.Created.class, Event.Serialization.JSON)");

        var refundSrc = generatedSourceOf(compilation, "kafka.multitopic.infrastructure.event.RefundCreatedEventTypeRegistrar");
        assertThat(refundSrc).contains("@Component(\"kafka_multitopic_RefundCreatedEventTypeRegistrar\")");
        assertThat(refundSrc).contains("registry.register(topic, Refund.Created.class, Event.Serialization.JSON)");
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
        assertThat(source).contains("@Value(\"${topic.user.primary}\") String[] userEventTopics0");
        assertThat(source).contains("@Value(\"${topic.user.secondary}\") String[] userEventTopics1");
        assertThat(source).contains("for (var topic : userEventTopics0)");
        assertThat(source).contains("for (var topic : userEventTopics1)");
        assertThat(source).contains("registry.register(topic, UserEvent.class, Event.Serialization.JSON)");
    }

    @Test
    void spelTopicExpressionIsInjectedInsteadOfRegisteredLiterally() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("kafka/speltopic/UserEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.speltopic.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Value(\"#{'${topic.user.names}'.split(',')}\") String[] userEventTopics0");
        assertThat(source).contains("@Value(\"prefab.${topic.env}.user\") String[] userEventTopics1");
        assertThat(source).contains("registry.register(\"prefab.user.audit\", UserEvent.class, Event.Serialization.JSON)");
        assertThat(source).doesNotContain("registry.register(\"#{");
        assertThat(source).doesNotContain("registry.register(\"prefab.${");
    }

    @Test
    void spelTopicExpressionExpandingToMultipleTopicsRegistersEachResolvedTopic() throws Exception {
        var classpath = compileDependencyClasspath(sourceOf("kafka/speltopic/UserEvent.java"));
        try (var classLoader = new URLClassLoader(new URL[] { classpath.toUri().toURL() }, getClass().getClassLoader());
             var applicationContext = new AnnotationConfigApplicationContext()) {
            applicationContext.setClassLoader(classLoader);
            applicationContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "topic.user.names", "prefab.user.created,prefab.user.updated",
                    "topic.env", "test")));
            applicationContext.registerBean(classLoader.loadClass(
                    "kafka.speltopic.infrastructure.event.UserEventEventTypeRegistrar"));
            applicationContext.refresh();

            var registry = new EventRegistry();
            applicationContext.getBeansOfType(EventRegistryCustomizer.class).values()
                    .forEach(customizer -> customizer.customize(registry));

            var eventType = classLoader.loadClass("kafka.speltopic.UserEvent");
            assertThat(registry.topicsForType(eventType)).containsExactlyInAnyOrder(
                    "prefab.user.created", "prefab.user.updated", "prefab.test.user", "prefab.user.audit");
            assertThat(registry.topicsWithSerialization(Event.Serialization.JSON)).containsExactlyInAnyOrder(
                    "prefab.user.created", "prefab.user.updated", "prefab.test.user", "prefab.user.audit");
            assertThat(registry.typeFor("prefab.user.updated")).isEqualTo(eventType);
        } finally {
            deleteRecursively(classpath);
        }
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
        assertThat(source).contains("registry.register(topic, MessageEvent.class, Event.Serialization.JSON)");
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
        assertThat(source).contains("registry.register(\"prefab.order\", OrderCreatedEvent.class, Event.Serialization.AVRO, event -> event.orderId())");
    }

    @Test
    void syntheticPartitioningKeyMethodOnRegularEventUsesMethodExtractor() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("kafka/synthetic/SyntheticOrderEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.synthetic.infrastructure.event.SyntheticOrderEventEventTypeRegistrar");
        assertThat(source).contains("registry.register(\"prefab.synthetic\", SyntheticOrderEvent.class, Event.Serialization.JSON, event -> event.tenantOrderKey())");
    }

    @Test
    void syntheticPartitioningKeyMethodOnAvscContractUsesSharedMethodExtractor() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscsynthetic/SyntheticOrderEvents.java"),
                        sourceOf("kafka/avscsynthetic/SyntheticOrderProcessor.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.avscsynthetic.infrastructure.event.SyntheticOrderCreatedEventTypeRegistrar");
        assertThat(source).contains("registry.register(\"prefab.synthetic.avsc\", SyntheticOrderCreated.class, Event.Serialization.AVRO, event -> event.tenantOrderKey())");
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
