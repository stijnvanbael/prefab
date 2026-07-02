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

class KafkaConsumerWriterTest {

    @Test
    void singleEventType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.single.infrastructure.kafka.UserExporterKafkaConsumer");
        assertThat(source).contains("topics = \"prefab.user\"");
        assertThat(source).contains("groupId = \"${spring.application.name}.user-exporter-on-user-created\"");
        assertThat(source).contains("public void onUserCreated(UserCreated event)");
        assertThat(source).contains("userExporter.onUserCreated(event)");
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
        var source = generatedSourceOf(compilation, "kafka.multiple.infrastructure.kafka.UserExporterKafkaConsumer");
        assertThat(source).contains("topics = \"${topic.user.name}\"");
        assertThat(source).contains("groupId = \"${spring.application.name}.user-exporter-on-user-event\"");
        assertThat(source).contains("public void onUserEvent(UserEvent event)");
        // switch dispatches to each sub-type handler
        assertThat(source).contains("case UserEvent.Created e -> userExporter.onUserCreated(e)");
        assertThat(source).contains("case UserEvent.Deleted e -> userExporter.onUserDeleted(e)");
        assertThat(source).contains("case UserEvent.Updated e -> userExporter.onUserUpdated(e)");
    }

    @Test
    void noParentEventType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/noparent/User.java"),
                        sourceOf("kafka/noparent/UserEvent.java"),
                        sourceOf("kafka/noparent/UserExporter.java"));
        assertThat(compilation).hadErrorContaining("share the same topic [user] but have no common ancestor");
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
        var source = generatedSourceOf(compilation, "kafka.multitopic.infrastructure.kafka.DayTotalKafkaConsumer");
        // two @KafkaListener methods, one per topic
        assertThat(source).contains("topics = \"${topic.sale.name}\"");
        assertThat(source).contains("public void onSaleCreated(Sale.Created event)");
        assertThat(source).contains("dayTotalService.onSaleCreated(event)");
        assertThat(source).contains("topics = \"${topic.refund.name}\"");
        assertThat(source).contains("public void onRefundCreated(Refund.Created event)");
        assertThat(source).contains("dayTotalService.onRefundCreated(event)");
    }

    @Test
    void multipleTopicsPerEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multitopicevent/UserEvent.java"),
                        sourceOf("kafka/multitopicevent/UserService.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.multitopicevent.infrastructure.kafka.UserServiceKafkaConsumer");
        // listener subscribes to both topics in a single annotation
        assertThat(source).contains("\"${topic.user.primary}\"");
        assertThat(source).contains("\"${topic.user.secondary}\"");
        assertThat(source).contains("public void onUserEvent(UserEvent event)");
        assertThat(source).contains("case UserEvent.Created e -> userService.onUserCreated(e)");
        assertThat(source).contains("case UserEvent.Updated e -> userService.onUserUpdated(e)");
    }

    @Test
    void consumeFromTopicsRestrictsSubscribedTopics() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/consumefromtopics/UserEvent.java"),
                        sourceOf("kafka/consumefromtopics/UserService.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.consumefromtopics.infrastructure.kafka.UserServiceKafkaConsumer");
        // only the primary topic is subscribed, secondary is excluded by @ConsumeFromTopics
        assertThat(source).contains("topics = \"${topic.user.primary}\"");
        assertThat(source).doesNotContain("topic.user.secondary");
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
        var source = generatedSourceOf(compilation, "kafka.customdlt.infrastructure.kafka.UserExporterKafkaConsumer");
        // a custom error handler is referenced in the listener when DLT is configured
        assertThat(source).contains("errorHandler = \"userExporterKafkaErrorHandler\"");
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
        var source = generatedSourceOf(compilation, "kafka.dltdisabled.infrastructure.kafka.UserExporterKafkaConsumer");
        // DLT disabled: still uses error handler for retries without dead-letter publishing
        assertThat(source).contains("errorHandler = \"userExporterKafkaErrorHandler\"");
    }

    @Test
    void autoOffsetResetOverride() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/offsetoverride/User.java"),
                        sourceOf("kafka/offsetoverride/UserCreated.java"),
                        sourceOf("kafka/offsetoverride/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.offsetoverride.infrastructure.kafka.UserExporterKafkaConsumer");
        // offset override must be propagated as a listener property
        assertThat(source).contains("properties = \"auto.offset.reset=${offset.override:latest}\"");
    }

    @Test
    void avscEventConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avsc/OrderCreated.java"),
                        sourceOf("kafka/avsc/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.avsc.infrastructure.kafka.OrderProcessorKafkaConsumer");
        assertThat(source).contains("topics = \"prefab.order\"");
        assertThat(source).contains("public void onOrderCreated(OrderCreatedEvent event)");
        assertThat(source).contains("orderProcessor.onOrderCreated(event)");
    }

    @Test
    void avscConcreteTypesConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscaggregate/OrderEvent.java"),
                        sourceOf("kafka/avscaggregate/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.avscaggregate.infrastructure.kafka.OrderProcessorKafkaConsumer");
        assertThat(source).contains("topics = \"prefab.order\"");
        assertThat(source).contains("public void onOrderEvent(OrderEvent event)");
        // concrete subtypes dispatched via switch
        assertThat(source).contains("case OrderCreatedEvent e -> orderProcessor.onOrderCreated(e)");
        assertThat(source).contains("case OrderShippedEvent e -> orderProcessor.onOrderShipped(e)");
    }

    @Test
    void avscMultipleEventConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscmulti/OrderEvent.java"),
                        sourceOf("kafka/avscmulti/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.avscmulti.infrastructure.kafka.OrderProcessorKafkaConsumer");
        assertThat(source).contains("topics = \"prefab.order\"");
        assertThat(source).contains("public void onOrderEvent(OrderEvent event)");
        // all events forwarded as-is to a single handler method
        assertThat(source).contains("orderProcessor.onOrderEvent(event)");
    }

    @Test
    void avscPartialEventConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscpartial/OrderEvent.java"),
                        sourceOf("kafka/avscpartial/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.avscpartial.infrastructure.kafka.OrderProcessorKafkaConsumer");
        assertThat(source).contains("topics = \"prefab.order\"");
        assertThat(source).contains("public void onOrderEvent(OrderEvent event)");
        // only OrderCreatedEvent is handled; other subtypes fall through to default
        assertThat(source).contains("case OrderCreatedEvent e -> orderProcessor.onOrderCreated(e)");
    }

    @Test
    void aggregateCreateOrUpdateHandler() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/createorupdate/ChannelSummary.java"),
                        sourceOf("kafka/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.createorupdate.infrastructure.kafka.ChannelSummaryKafkaConsumer");
        assertThat(source).contains("topics = \"${topic.message.name}\"");
        assertThat(source).contains("public void onMessageEvent(MessageEvent event)");
        assertThat(source).contains("case MessageEvent.Sent e -> channelSummaryService.onUpdate(e)");
    }

    @Test
    void asyncCommitAggregateConsumerIsTransactional() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("kafka/asynccommit/Order.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.asynccommit.infrastructure.kafka.OrderKafkaConsumer")
                .contentsAsUtf8String()
                .contains("@Transactional");
    }

    @Test
    void avscAsyncCommitAggregateGeneratesSchemaFactoryAndConverters() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscasynccommit/OrderPlaced.java"),
                        sourceOf("kafka/avscasynccommit/Order.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("kafka.avscasynccommit.infrastructure.avro.OrderPlacedEventSchemaFactory")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("kafka.avscasynccommit.infrastructure.avro.OrderPlacedEventToGenericRecordConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("kafka.avscasynccommit.infrastructure.avro.GenericRecordToOrderPlacedEventConverter")
                .isNotNull();
        assertThat(compilation)
                .generatedSourceFile("kafka.avscasynccommit.infrastructure.kafka.OrderKafkaConsumer")
                .contentsAsUtf8String()
                .contains("@Transactional");
    }

    @Test
    void eventTypeFromDependencyModule() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("kafka/dependencyevents/ExternalUserCreated.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("kafka/externaldependency/UserImporter.java"));
            assertThat(compilation).succeeded();
            var source = generatedSourceOf(compilation, "kafka.externaldependency.infrastructure.kafka.UserImporterKafkaConsumer");
            assertThat(source).contains("import kafka.dependencyevents.ExternalUserCreated");
            assertThat(source).contains("topics = \"prefab.external.user\"");
            assertThat(source).contains("public void onExternalUserCreated(ExternalUserCreated event)");
            assertThat(source).contains("userImporter.onExternalUserCreated(event)");
        } finally {
            deleteRecursively(dependencyClasspath);
        }
    }

    @Test
    void avscEventTypeFromDependencyModule() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("kafka/dependencyavsc/ExternalOrderCreated.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("kafka/externaldependencyavsc/ExternalOrderImporter.java"));
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedSourceFile("kafka.externaldependencyavsc.infrastructure.kafka.ExternalOrderImporterKafkaConsumer")
                    .contentsAsUtf8String()
                    .contains("import kafka.dependencyavsc.ExternalOrderCreatedEvent;");
            assertThat(compilation)
                    .generatedSourceFile("kafka.externaldependencyavsc.infrastructure.kafka.ExternalOrderImporterKafkaConsumer")
                    .contentsAsUtf8String()
                    .contains("topics = \"prefab.external.order\"");
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
