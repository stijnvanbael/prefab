package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class SqsSubscriberWriterTest {

    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/single/User.java"),
                        sourceOf("sns/single/UserCreated.java"),
                        sourceOf("sns/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.single.infrastructure.sns.UserExporterSqsSubscriber");
        assertThat(source).contains("SqsSubscriptionRequest<UserCreated>(\"user\", \"user-exporter-on-user-created\", UserCreated.class");
        assertThat(source).contains("private void onUserCreated(UserCreated event)");
        assertThat(source).contains("userExporter.onUserCreated(event)");
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/multiple/User.java"),
                        sourceOf("sns/multiple/UserEvent.java"),
                        sourceOf("sns/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.multiple.infrastructure.sns.UserExporterSqsSubscriber");
        assertThat(source).contains("SqsSubscriptionRequest<UserEvent>(userEventTopic, \"user-exporter-on-user-event\", UserEvent.class");
        assertThat(source).contains("private void onUserEvent(UserEvent event)");
        assertThat(source).contains("case UserEvent.Created e -> userExporter.onUserCreated(e)");
        assertThat(source).contains("case UserEvent.Deleted e -> userExporter.onUserDeleted(e)");
        assertThat(source).contains("case UserEvent.Updated e -> userExporter.onUserUpdated(e)");
    }

    @Test
    void noParentEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/noparent/User.java"),
                        sourceOf("sns/noparent/UserEvent.java"),
                        sourceOf("sns/noparent/UserExporter.java"));
        assertThat(compilation).hadErrorContaining("share the same topic [user] but have no common ancestor");
    }

    @Test
    void multipleTopics() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/multitopic/Sale.java"),
                        sourceOf("sns/multitopic/Refund.java"),
                        sourceOf("sns/multitopic/DayTotal.java"),
                        sourceOf("sns/multitopic/DayTotalRepositoryMixin.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.multitopic.infrastructure.sns.DayTotalSqsSubscriber");
        assertThat(source).contains("SqsSubscriptionRequest<Sale.Created>(saleCreatedTopic, \"day-total-on-sale-created\", Sale.Created.class");
        assertThat(source).contains("private void onSaleCreated(Sale.Created event)");
        assertThat(source).contains("dayTotalService.onSaleCreated(event)");
        assertThat(source).contains("SqsSubscriptionRequest<Refund.Created>(refundCreatedTopic, \"day-total-on-refund-created\", Refund.Created.class");
        assertThat(source).contains("private void onRefundCreated(Refund.Created event)");
        assertThat(source).contains("dayTotalService.onRefundCreated(event)");
    }

    @Test
    void multipleTopicsPerEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/multitopicevent/UserEvent.java"),
                        sourceOf("sns/multitopicevent/UserService.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.multitopicevent.infrastructure.sns.UserServiceSqsSubscriber");
        // one subscription per topic, same handler method
        assertThat(source).contains("SqsSubscriptionRequest<UserEvent>(userEvent0Topic, \"user-service-on-user-event\", UserEvent.class");
        assertThat(source).contains("SqsSubscriptionRequest<UserEvent>(userEvent1Topic, \"user-service-on-user-event\", UserEvent.class");
        assertThat(source).contains("private void onUserEvent(UserEvent event)");
        assertThat(source).contains("case UserEvent.Created e -> userService.onUserCreated(e)");
        assertThat(source).contains("case UserEvent.Updated e -> userService.onUserUpdated(e)");
    }

    @Test
    void customDlt() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/customdlt/User.java"),
                        sourceOf("sns/customdlt/UserEvent.java"),
                        sourceOf("sns/customdlt/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.customdlt.infrastructure.sns.UserExporterSqsSubscriber");
        // custom DLT: dead-letter queue name and retry template are configured
        assertThat(source).contains(".withDeadLetterQueueName(deadLetterTopic)");
        assertThat(source).contains("${custom.dlt.name}");
        assertThat(source).contains(".withRetryTemplate(");
    }

    @Test
    void dltDisabled() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/dltdisabled/User.java"),
                        sourceOf("sns/dltdisabled/UserEvent.java"),
                        sourceOf("sns/dltdisabled/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.dltdisabled.infrastructure.sns.UserExporterSqsSubscriber");
        // DLT disabled: null dead-letter queue name, no retry template
        assertThat(source).contains(".withDeadLetterQueueName(null)");
        assertThat(source).doesNotContain(".withRetryTemplate(");
    }

    @Test
    void aggregateCreateOrUpdateHandler() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/createorupdate/ChannelSummary.java"),
                        sourceOf("sns/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.createorupdate.infrastructure.sns.ChannelSummarySqsSubscriber");
        assertThat(source).contains("SqsSubscriptionRequest<MessageEvent>(messageEventTopic, \"channel-summary-on-message-event\", MessageEvent.class");
        assertThat(source).contains("private void onMessageEvent(MessageEvent event)");
        assertThat(source).contains("case MessageEvent.Sent e -> channelSummaryService.onUpdate(e)");
    }
}
