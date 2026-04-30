package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

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
        assertThat(compilation).generatedSourceFile("kafka.single.infrastructure.kafka.UserExporterKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/single/UserExporterKafkaConsumer.java"));
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
        assertThat(compilation).generatedSourceFile("kafka.multiple.infrastructure.kafka.UserExporterKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/multiple/UserExporterKafkaConsumer.java"));
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
        assertThat(compilation).generatedSourceFile("kafka.multitopic.infrastructure.kafka.DayTotalKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/multitopic/DayTotalKafkaConsumer.java"));
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
        assertThat(compilation).generatedSourceFile("kafka.customdlt.infrastructure.kafka.UserExporterKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/customdlt/UserExporterKafkaConsumer.java"));
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
        assertThat(compilation).generatedSourceFile("kafka.dltdisabled.infrastructure.kafka.UserExporterKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/dltdisabled/UserExporterKafkaConsumer.java"));
    }

    @Test
    void avscEventConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avsc/OrderCreated.java"),
                        sourceOf("kafka/avsc/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.avsc.infrastructure.kafka.OrderProcessorKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/avsc/OrderProcessorKafkaConsumer.java"));
    }

    @Test
    void avscConcreteTypesConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscaggregate/OrderEvent.java"),
                        sourceOf("kafka/avscaggregate/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.avscaggregate.infrastructure.kafka.OrderProcessorKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/avscaggregate/OrderProcessorKafkaConsumer.java"));
    }

    @Test
    void avscMultipleEventConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscmulti/OrderEvent.java"),
                        sourceOf("kafka/avscmulti/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.avscmulti.infrastructure.kafka.OrderProcessorKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/avscmulti/OrderProcessorKafkaConsumer.java"));
    }

    @Test
    void avscPartialEventConsumer() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/avscpartial/OrderEvent.java"),
                        sourceOf("kafka/avscpartial/OrderProcessor.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.avscpartial.infrastructure.kafka.OrderProcessorKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/avscpartial/OrderProcessorKafkaConsumer.java"));
    }

    @Test
    void aggregateCreateOrUpdateHandler() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/createorupdate/ChannelSummary.java"),
                        sourceOf("kafka/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.createorupdate.infrastructure.kafka.ChannelSummaryKafkaConsumer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/createorupdate/ChannelSummaryKafkaConsumer.java"));
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
}
