package be.appify.prefab.processor.pubsub;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class PubSubSubscriberWriterTest {

    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/single/User.java"),
                        sourceOf("pubsub/single/UserCreated.java"),
                        sourceOf("pubsub/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.single.infrastructure.pubsub.UserExporterPubSubSubscriber");
        assertThat(source).contains("SubscriptionRequest<UserCreated>(\"user\", \"user-exporter-on-user-created\", UserCreated.class");
        assertThat(source).contains("private void onUserCreated(UserCreated event)");
        assertThat(source).contains("userExporter.onUserCreated(event)");
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/multiple/User.java"),
                        sourceOf("pubsub/multiple/UserEvent.java"),
                        sourceOf("pubsub/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.multiple.infrastructure.pubsub.UserExporterPubSubSubscriber");
        assertThat(source).contains("SubscriptionRequest<UserEvent>(userEventTopic, \"user-exporter-on-user-event\", UserEvent.class");
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
                        sourceOf("pubsub/noparent/User.java"),
                        sourceOf("pubsub/noparent/UserEvent.java"),
                        sourceOf("pubsub/noparent/UserExporter.java"));
        assertThat(compilation).hadErrorContaining("share the same topic [user] but have no common ancestor");
    }

    @Test
    void multipleTopics() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/multitopic/Sale.java"),
                        sourceOf("pubsub/multitopic/Refund.java"),
                        sourceOf("pubsub/multitopic/DayTotal.java"),
                        sourceOf("pubsub/multitopic/DayTotalRepositoryMixin.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.multitopic.infrastructure.pubsub.DayTotalPubSubSubscriber");
        // one subscription per topic
        assertThat(source).contains("SubscriptionRequest<Sale.Created>(saleCreatedTopic, \"day-total-on-sale-created\", Sale.Created.class");
        assertThat(source).contains("private void onSaleCreated(Sale.Created event)");
        assertThat(source).contains("dayTotalService.onSaleCreated(event)");
        assertThat(source).contains("SubscriptionRequest<Refund.Created>(refundCreatedTopic, \"day-total-on-refund-created\", Refund.Created.class");
        assertThat(source).contains("private void onRefundCreated(Refund.Created event)");
        assertThat(source).contains("dayTotalService.onRefundCreated(event)");
    }

    @Test
    void multipleTopicsPerEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/multitopicevent/UserEvent.java"),
                        sourceOf("pubsub/multitopicevent/UserService.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.multitopicevent.infrastructure.pubsub.UserServicePubSubSubscriber");
        // one subscription per topic, same handler method
        assertThat(source).contains("SubscriptionRequest<UserEvent>(userEvent0Topic, \"user-service-on-user-event\", UserEvent.class");
        assertThat(source).contains("SubscriptionRequest<UserEvent>(userEvent1Topic, \"user-service-on-user-event\", UserEvent.class");
        assertThat(source).contains("private void onUserEvent(UserEvent event)");
        assertThat(source).contains("case UserEvent.Created e -> userService.onUserCreated(e)");
        assertThat(source).contains("case UserEvent.Updated e -> userService.onUserUpdated(e)");
    }

    @Test
    void customDlt() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/customdlt/User.java"),
                        sourceOf("pubsub/customdlt/UserEvent.java"),
                        sourceOf("pubsub/customdlt/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.customdlt.infrastructure.pubsub.UserExporterPubSubSubscriber");
        // custom DLT: dead-letter policy and retry template are configured
        assertThat(source).contains(".withDeadLetterPolicy(");
        assertThat(source).contains("${custom.dlt.name}");
        assertThat(source).contains(".withRetryTemplate(");
    }

    @Test
    void dltDisabled() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/dltdisabled/User.java"),
                        sourceOf("pubsub/dltdisabled/UserEvent.java"),
                        sourceOf("pubsub/dltdisabled/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.dltdisabled.infrastructure.pubsub.UserExporterPubSubSubscriber");
        // DLT disabled: null dead-letter policy, no retry template
        assertThat(source).contains(".withDeadLetterPolicy(null)");
        assertThat(source).doesNotContain(".withRetryTemplate(");
    }

    @Test
    void aggregateCreateOrUpdateHandler() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/createorupdate/ChannelSummary.java"),
                        sourceOf("pubsub/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.createorupdate.infrastructure.pubsub.ChannelSummaryPubSubSubscriber");
        assertThat(source).contains("SubscriptionRequest<MessageEvent>(messageEventTopic, \"channel-summary-on-message-event\", MessageEvent.class");
        assertThat(source).contains("private void onMessageEvent(MessageEvent event)");
        assertThat(source).contains("channelSummaryService.onUpdate(event)");
        assertThat(source).doesNotContain("case MessageEvent.Sent e ->");
    }
}
