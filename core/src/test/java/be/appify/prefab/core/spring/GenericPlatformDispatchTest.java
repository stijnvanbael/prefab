package be.appify.prefab.core.spring;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.GenericKafkaProducer;
import be.appify.prefab.core.pubsub.GenericPubSubPublisher;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.sns.GenericSnsPublisher;
import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsUtil;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenericPlatformDispatchTest {
    @Event(topic = "kafka", typeArguments = String.class, platform = Event.Platform.KAFKA)
    @Event(topic = "pubsub", typeArguments = Integer.class, platform = Event.Platform.PUB_SUB)
    @Event(topic = "sns", typeArguments = Long.class, platform = Event.Platform.SNS_SQS)
    public record Changed<T>(T payload) {}

    @AfterEach
    void resetPublisher() {
        DomainEventPublisher.reset();
    }

    @Test
    void concreteDispatchSelectsOnlyTheBindingsDeclaredPlatform() {
        var kafka = new EventType<Changed<String>>() {};
        var pubsub = new EventType<Changed<Integer>>() {};
        var sns = new EventType<Changed<Long>>() {};
        var registry = new EventRegistry();
        registry.register("kafka", kafka, Event.Serialization.JSON);
        registry.register("pubsub", pubsub, Event.Serialization.JSON);
        registry.register("sns", sns, Event.Serialization.JSON);
        var pubSubUtil = new PubSubUtil("project", "app", "", 0, 1, 1, 1.0f,
                null, null, null, registry);
        var sqsUtil = new SqsUtil("app", "", 0, 1, 1, 1.0, null, null, null, registry);
        var kafkaPublisher = new GenericKafkaProducer(null, registry);
        var pubSubPublisher = new GenericPubSubPublisher(null, pubSubUtil, null);
        var snsPublisher = new GenericSnsPublisher(null, sqsUtil, null);

        assertThat(kafkaPublisher.canDispatch(kafka)).isTrue();
        assertThat(kafkaPublisher.canDispatch(pubsub)).isFalse();
        assertThat(pubSubPublisher.canDispatch(pubsub)).isTrue();
        assertThat(pubSubPublisher.canDispatch(sns)).isFalse();
        assertThat(snsPublisher.canDispatch(sns)).isTrue();
        assertThat(snsPublisher.canDispatch(kafka)).isFalse();
    }

    @Test
    void springUsesTheConcreteDispatcherOverload() {
        var type = new EventType<Changed<Long>>() {};
        var registry = new EventRegistry();
        registry.register("sns", type, Event.Serialization.JSON);
        var kafkaPublisher = new GenericKafkaProducer(null, registry);
        var util = mock(SqsUtil.class);
        var template = mock(SnsTemplate.class);
        var serializer = mock(SnsSerializer.class);
        when(util.hasTopicForType(type)).thenReturn(true);
        when(util.topicsForDispatch(any())).thenReturn(List.of("sns"));
        when(util.ensureTopicExists("sns")).thenReturn("arn:sns");
        when(serializer.serialize(eq("sns"), any())).thenReturn("{\"payload\":1}");
        var springBus = mock(org.springframework.context.ApplicationEventPublisher.class);
        new SpringDomainEventPublisher(springBus,
                List.of(kafkaPublisher, new GenericSnsPublisher(template, util, serializer)));

        EventPublisher.bind(type).publish(new Changed<>(1L));

        verify(template).sendNotification(eq("arn:sns"), any(SnsNotification.class));
        verifyNoInteractions(springBus);
    }
}
