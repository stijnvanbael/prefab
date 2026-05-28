package be.appify.prefab.core.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericPubSubPublisherTest {

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private PubSubUtil pubSubUtil;

    @Mock
    private PubSubSerializer serializer;

    private GenericPubSubPublisher publisher;

    private record TestEvent(String id) {}

    @BeforeEach
    void setUp() {
        publisher = new GenericPubSubPublisher(pubSubTemplate, pubSubUtil, serializer);
        when(pubSubUtil.ensureTopicExists(anyString())).thenAnswer(inv -> "projects/test/" + inv.getArgument(0));
        when(serializer.serialize(anyString(), any())).thenReturn(new byte[]{1, 2, 3});
        when(pubSubUtil.keyFor(any())).thenReturn(Optional.empty());
        when(pubSubTemplate.publish(anyString(), any(PubsubMessage.class)))
                .thenReturn(CompletableFuture.completedFuture("msg-id"));
    }

    @Test
    @DisplayName("dispatch_givenNoOverrides_publishesToRegistryTopics")
    void dispatch_givenNoOverrides_publishesToRegistryTopics() {
        var event = new TestEvent("1");
        when(pubSubUtil.topicsForDispatch(event)).thenReturn(List.of("registered-topic"));

        publisher.dispatch(event);

        verify(pubSubTemplate).publish(org.mockito.ArgumentMatchers.eq("projects/test/registered-topic"),
                any(PubsubMessage.class));
    }

    @Test
    @DisplayName("dispatch_givenSingleTopicOverride_publishesToOverrideTopic")
    void dispatch_givenSingleTopicOverride_publishesToOverrideTopic() {
        var event = new TestEvent("2");

        publisher.dispatch(event, "override-topic");

        verify(pubSubTemplate).publish(org.mockito.ArgumentMatchers.eq("projects/test/override-topic"),
                any(PubsubMessage.class));
        // topicsForDispatch must NOT be consulted when overrides are given
        verify(pubSubUtil, org.mockito.Mockito.never()).topicsForDispatch(any());
    }

    @Test
    @DisplayName("dispatch_givenMultipleTopicOverrides_publishesToAllOverrideTopics")
    void dispatch_givenMultipleTopicOverrides_publishesToAllOverrideTopics() {
        var event = new TestEvent("3");

        publisher.dispatch(event, "topic-a", "topic-b");

        verify(pubSubTemplate).publish(org.mockito.ArgumentMatchers.eq("projects/test/topic-a"),
                any(PubsubMessage.class));
        verify(pubSubTemplate).publish(org.mockito.ArgumentMatchers.eq("projects/test/topic-b"),
                any(PubsubMessage.class));
        verify(pubSubUtil, org.mockito.Mockito.never()).topicsForDispatch(any());
    }

    @Test
    @DisplayName("dispatch_givenMultipleRegisteredTopics_publishesToAllFromRegistry")
    void dispatch_givenMultipleRegisteredTopics_publishesToAllFromRegistry() {
        var event = new TestEvent("4");
        when(pubSubUtil.topicsForDispatch(event)).thenReturn(List.of("primary", "secondary"));

        publisher.dispatch(event);

        verify(pubSubTemplate, times(2)).publish(anyString(), any(PubsubMessage.class));
    }

    @Test
    @DisplayName("dispatch_setsEventTypeAttribute")
    void dispatch_setsEventTypeAttribute() {
        var event = new TestEvent("5");
        when(pubSubUtil.topicsForDispatch(event)).thenReturn(List.of("my-topic"));
        var captor = ArgumentCaptor.forClass(PubsubMessage.class);

        publisher.dispatch(event);

        verify(pubSubTemplate).publish(anyString(), captor.capture());
        assertThat(captor.getValue().getAttributesMap())
                .containsEntry("type", TestEvent.class.getName());
    }
}

