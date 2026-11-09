package be.appify.prefab.core.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.sns.SqsUtil;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaAdmin;

class GenericEventProvisioningTest {
    @Test
    void startupProvisionsEveryPublisherOnlyBindingAndFanOutTargetOnItsOwnPlatform() {
        var registry = new EventRegistry();
        var strings = new EventType<Changed<String>>() {};
        registry.register("first", strings, Event.Serialization.JSON);
        registry.register("second", strings, Event.Serialization.JSON);
        registry.register("pubsub", new EventType<Changed<Integer>>() {}, Event.Serialization.JSON);
        registry.register("sns", new EventType<Changed<Long>>() {}, Event.Serialization.JSON);
        registry.register("second", new EventType<Changed<Double>>() {}, Event.Serialization.JSON);
        registry.register("ordinary", String.class, Event.Serialization.JSON);
        var kafka = mock(KafkaAdmin.class);
        var pubsub = mock(PubSubUtil.class);
        var sns = mock(SqsUtil.class);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GenericEventProvisioningConfiguration.class))
                .withBean(EventRegistry.class, () -> registry)
                .withBean(KafkaAdmin.class, () -> kafka)
                .withBean(PubSubUtil.class, () -> pubsub)
                .withBean(SqsUtil.class, () -> sns)
                .run(context -> assertThat(context).hasNotFailed());

        var topics = ArgumentCaptor.forClass(NewTopic.class);
        verify(kafka, times(2)).createOrModifyTopics(topics.capture());
        assertThat(topics.getAllValues().stream().map(NewTopic::name)).containsExactly("first", "second");
        assertThat(topics.getAllValues()).allSatisfy(topic -> {
            assertThat(topic.numPartitions()).isEqualTo(-1);
            assertThat(topic.replicationFactor()).isEqualTo((short) -1);
        });
        verify(pubsub).ensureTopicExists("pubsub");
        verify(sns).ensureTopicExists("sns");
        verify(sns).destroy();
        verifyNoMoreInteractions(pubsub, sns);
    }

    @Test
    void ordinaryApplicationsDoNotChangeTopicProvisioning() {
        var registry = new EventRegistry();
        registry.register("ordinary", String.class, Event.Serialization.JSON);
        var kafka = mock(KafkaAdmin.class);
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GenericEventProvisioningConfiguration.class))
                .withBean(EventRegistry.class, () -> registry)
                .withBean(KafkaAdmin.class, () -> kafka)
                .run(context -> assertThat(context).hasNotFailed());
        verify(kafka, never()).createOrModifyTopics(any(NewTopic[].class));
    }

    @Event(topic = {"first", "second"}, platform = Event.Platform.KAFKA, typeArguments = String.class)
    @Event(topic = "pubsub", platform = Event.Platform.PUB_SUB, typeArguments = Integer.class)
    @Event(topic = "sns", platform = Event.Platform.SNS_SQS, typeArguments = Long.class)
    @Event(topic = "second", platform = Event.Platform.KAFKA, typeArguments = Double.class)
    record Changed<T>(T value) {}
}
