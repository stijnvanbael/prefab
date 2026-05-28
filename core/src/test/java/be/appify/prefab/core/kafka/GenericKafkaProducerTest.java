package be.appify.prefab.core.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private EventRegistry eventRegistry;

    private GenericKafkaProducer producer;

    private record TestEvent(String id) {}

    @BeforeEach
    void setUp() {
        producer = new GenericKafkaProducer(kafkaTemplate, eventRegistry);
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(eventRegistry.keyFor(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("dispatch_givenNoOverrides_publishesToRegistryTopics")
    void dispatch_givenNoOverrides_publishesToRegistryTopics() {
        var event = new TestEvent("1");
        when(eventRegistry.topicsForDispatch(event)).thenReturn(List.of("registered-topic"));

        producer.dispatch(event);

        verify(kafkaTemplate).send("registered-topic", null, event);
    }

    @Test
    @DisplayName("dispatch_givenSingleTopicOverride_publishesToOverrideTopic")
    void dispatch_givenSingleTopicOverride_publishesToOverrideTopic() {
        var event = new TestEvent("2");

        producer.dispatch(event, "override-topic");

        verify(kafkaTemplate).send("override-topic", null, event);
        // topicsForDispatch must NOT be consulted when overrides are given
        verify(eventRegistry, never()).topicsForDispatch(any());
    }

    @Test
    @DisplayName("dispatch_givenMultipleTopicOverrides_publishesToAllOverrideTopics")
    void dispatch_givenMultipleTopicOverrides_publishesToAllOverrideTopics() {
        var event = new TestEvent("3");

        producer.dispatch(event, "topic-a", "topic-b");

        verify(kafkaTemplate).send("topic-a", null, event);
        verify(kafkaTemplate).send("topic-b", null, event);
        verify(eventRegistry, never()).topicsForDispatch(any());
    }

    @Test
    @DisplayName("dispatch_givenAllStrategy_publishesToAllRegisteredTopics")
    void dispatch_givenAllStrategy_publishesToAllRegisteredTopics() {
        var event = new TestEvent("4");
        when(eventRegistry.topicsForDispatch(event)).thenReturn(List.of("primary", "secondary"));

        producer.dispatch(event);

        verify(kafkaTemplate).send("primary", null, event);
        verify(kafkaTemplate).send("secondary", null, event);
    }
}
