package be.appify.prefab.core.sns;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericSnsPublisherTest {

    @Mock
    private SnsTemplate snsTemplate;

    @Mock
    private SqsUtil sqsUtil;

    @Mock
    private SnsSerializer snsSerializer;

    private GenericSnsPublisher publisher;

    private record TestEvent(String id) {}

    @BeforeEach
    void setUp() {
        publisher = new GenericSnsPublisher(snsTemplate, sqsUtil, snsSerializer);
        when(sqsUtil.ensureTopicExists(anyString())).thenAnswer(inv -> "arn:aws:sns:us-east-1:000000000000:" + inv.getArgument(0));
        when(snsSerializer.serialize(anyString(), any())).thenReturn("{\"id\":\"test\"}");
    }

    @Test
    @DisplayName("dispatch_givenNoOverrides_publishesToRegistryTopics")
    void dispatch_givenNoOverrides_publishesToRegistryTopics() {
        var event = new TestEvent("1");
        when(sqsUtil.topicsForDispatch(event)).thenReturn(List.of("registered-topic"));

        publisher.dispatch(event);

        verify(snsTemplate).sendNotification(
                "arn:aws:sns:us-east-1:000000000000:registered-topic",
                "{\"id\":\"test\"}",
                TestEvent.class.getName());
    }

    @Test
    @DisplayName("dispatch_givenSingleTopicOverride_publishesToOverrideTopic")
    void dispatch_givenSingleTopicOverride_publishesToOverrideTopic() {
        var event = new TestEvent("2");

        publisher.dispatch(event, "override-topic");

        verify(snsTemplate).sendNotification(
                "arn:aws:sns:us-east-1:000000000000:override-topic",
                "{\"id\":\"test\"}",
                TestEvent.class.getName());
        verify(sqsUtil, never()).topicsForDispatch(any());
    }

    @Test
    @DisplayName("dispatch_givenMultipleTopicOverrides_publishesToAllOverrideTopics")
    void dispatch_givenMultipleTopicOverrides_publishesToAllOverrideTopics() {
        var event = new TestEvent("3");

        publisher.dispatch(event, "topic-a", "topic-b");

        verify(snsTemplate).sendNotification(
                "arn:aws:sns:us-east-1:000000000000:topic-a",
                "{\"id\":\"test\"}",
                TestEvent.class.getName());
        verify(snsTemplate).sendNotification(
                "arn:aws:sns:us-east-1:000000000000:topic-b",
                "{\"id\":\"test\"}",
                TestEvent.class.getName());
        verify(sqsUtil, never()).topicsForDispatch(any());
    }

    @Test
    @DisplayName("dispatch_givenMultipleRegisteredTopics_publishesToAllFromRegistry")
    void dispatch_givenMultipleRegisteredTopics_publishesToAllFromRegistry() {
        var event = new TestEvent("4");
        when(sqsUtil.topicsForDispatch(event)).thenReturn(List.of("primary", "secondary"));

        publisher.dispatch(event);

        verify(snsTemplate).sendNotification(
                "arn:aws:sns:us-east-1:000000000000:primary",
                "{\"id\":\"test\"}",
                TestEvent.class.getName());
        verify(snsTemplate).sendNotification(
                "arn:aws:sns:us-east-1:000000000000:secondary",
                "{\"id\":\"test\"}",
                TestEvent.class.getName());
    }
}

