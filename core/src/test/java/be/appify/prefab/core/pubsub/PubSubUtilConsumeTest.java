package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.kafka.EventRegistry;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that message processing never terminates or starves the subscriber and that production retry +
 * dead-letter semantics are retained: consumer failure retries, then negative-acknowledges for redelivery and
 * dead-lettering, never acknowledging, never rethrowing, while a successful consumer is acknowledged exactly once.
 */
class PubSubUtilConsumeTest {

    private final PubSubAdmin admin = mock(PubSubAdmin.class);
    private final PubSubSubscriberTemplate subscriberTemplate = mock(PubSubSubscriberTemplate.class);

    private PubSubUtil pubSubUtil(int maxRetries) {
        when(admin.getTopic(anyString())).thenReturn(Topic.getDefaultInstance());
        when(admin.getSubscription(anyString())).thenReturn(Subscription.getDefaultInstance());
        doReturn(mock(Subscriber.class)).when(subscriberTemplate).subscribe(anyString(), any());
        return new PubSubUtil("project", "app", "", maxRetries, 1, 1, 1.0f,
                admin, subscriberTemplate, null, new EventRegistry());
    }

    @SuppressWarnings("unchecked")
    private Consumer<BasicAcknowledgeablePubsubMessage> receiver(PubSubUtil util, Consumer<PubsubMessage> consumer) {
        util.subscribe(SubscriptionRequest.raw("topic", "subscription", consumer));
        ArgumentCaptor<Consumer<BasicAcknowledgeablePubsubMessage>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(subscriberTemplate).subscribe(anyString(), callback.capture());
        return callback.getValue();
    }

    private static BasicAcknowledgeablePubsubMessage message() {
        var message = mock(BasicAcknowledgeablePubsubMessage.class);
        when(message.getPubsubMessage()).thenReturn(PubsubMessage.getDefaultInstance());
        when(message.ack()).thenReturn(CompletableFuture.completedFuture(null));
        when(message.nack()).thenReturn(CompletableFuture.completedFuture(null));
        return message;
    }

    @Test
    void successfulConsumerIsAcknowledgedOnceAndNotNacked() {
        var util = pubSubUtil(5);
        var calls = new AtomicInteger();
        var receiver = receiver(util, message -> calls.incrementAndGet());
        var message = message();

        assertThatCode(() -> receiver.accept(message)).doesNotThrowAnyException();

        assertThat(calls.get()).isEqualTo(1);
        verify(message, times(1)).ack();
        verify(message, never()).nack();
    }

    @Test
    void consumerFailureRetriesThenNacksAndNeverRethrowsOrAcks() {
        var util = pubSubUtil(2); // two retries after the initial attempt
        var calls = new AtomicInteger();
        var receiver = receiver(util, message -> {
            calls.incrementAndGet();
            throw new IllegalStateException("No aggregates found for event"); // e.g. missing projection
        });
        var message = message();

        assertThatCode(() -> receiver.accept(message)).doesNotThrowAnyException();

        assertThat(calls.get()).isEqualTo(3); // initial attempt + 2 retries
        verify(message, never()).ack();
        verify(message, times(1)).nack(); // handed back to Pub/Sub for redelivery + dead-letter
    }

    @Test
    void consumerSucceedingOnRetryAcksOnceWithoutNacking() {
        var util = pubSubUtil(5);
        var calls = new AtomicInteger();
        var receiver = receiver(util, message -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
        });
        var message = message();

        receiver.accept(message);

        assertThat(calls.get()).isEqualTo(3);
        verify(message, times(1)).ack();
        verify(message, never()).nack();
    }

    @Test
    void successfulConsumerIsNotReInvokedWhenAckFails() {
        var util = pubSubUtil(5);
        var calls = new AtomicInteger();
        var receiver = receiver(util, message -> calls.incrementAndGet());
        var message = message();
        when(message.ack()).thenThrow(new IllegalStateException("ack transport failed"))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThatCode(() -> receiver.accept(message)).doesNotThrowAnyException();

        assertThat(calls.get()).isEqualTo(1); // consumer success is not re-run
        verify(message, times(1)).ack();
        verify(message, never()).nack();
    }

    @Test
    void errorInConsumerIsCaughtAndNacked() {
        var util = pubSubUtil(0);
        var receiver = receiver(util, message -> {
            throw new AssertionError("boom");
        });
        var message = message();

        assertThatCode(() -> receiver.accept(message)).doesNotThrowAnyException();

        verify(message, never()).ack();
        verify(message, times(1)).nack();
    }

    @Test
    void nackFailureDoesNotPropagate() {
        var util = pubSubUtil(0);
        var receiver = receiver(util, message -> {
            throw new IllegalStateException("boom");
        });
        var message = message();
        when(message.nack()).thenThrow(new IllegalStateException("nack transport failed"));

        assertThatCode(() -> receiver.accept(message)).doesNotThrowAnyException();
    }

    @Test
    void aFailingMessageDoesNotPreventSubsequentMessagesFromBeingProcessed() {
        var util = pubSubUtil(1);
        var processed = new ArrayList<String>();
        var receiver = receiver(util, message -> {
            if (message.getData().toStringUtf8().equals("poison")) {
                throw new IllegalStateException("No aggregates found for event");
            }
            processed.add(message.getData().toStringUtf8());
        });

        receiver.accept(rawMessage("poison"));
        receiver.accept(rawMessage("valid"));

        assertThat(processed).containsExactly("valid");
    }

    private static BasicAcknowledgeablePubsubMessage rawMessage(String data) {
        var message = mock(BasicAcknowledgeablePubsubMessage.class);
        when(message.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder()
                .setData(com.google.protobuf.ByteString.copyFromUtf8(data)).build());
        when(message.ack()).thenReturn(CompletableFuture.completedFuture(null));
        when(message.nack()).thenReturn(CompletableFuture.completedFuture(null));
        return message;
    }
}
