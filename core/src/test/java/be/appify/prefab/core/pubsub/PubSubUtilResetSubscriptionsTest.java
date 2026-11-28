package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.kafka.EventRegistry;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PubSubUtil#resetSubscriptions()} isolates tests: stop delivery, settle in-flight work, discard backlog,
 * recreate the subscription empty and re-subscribe, without touching production retry/dead-letter policy.
 */
class PubSubUtilResetSubscriptionsTest {

    @Test
    void resetStopsRecreatesAndResubscribesEverySubscription() {
        var admin = mock(PubSubAdmin.class);
        var subscriberTemplate = mock(PubSubSubscriberTemplate.class);
        var first = mock(Subscriber.class);
        var second = mock(Subscriber.class);
        when(admin.getTopic(anyString())).thenReturn(Topic.getDefaultInstance());
        // first call: subscription absent -> created; subsequent checks find it
        when(admin.getSubscription(anyString()))
                .thenReturn(null)
                .thenReturn(Subscription.getDefaultInstance());
        doReturn(first, second, mock(Subscriber.class)).when(subscriberTemplate).subscribe(anyString(), any());
        var util = new PubSubUtil("project", "app", "", 5, 1, 1, 1.0f,
                admin, subscriberTemplate, null, new EventRegistry());

        util.subscribe(new SubscriptionRequest<PubsubMessage>("topic", "subscription", PubsubMessage.class, e -> { })
                .withExecutor(Runnable::run));
        util.subscribe(SubscriptionRequest.raw("other-topic", "raw-subscription", message -> { }));

        util.resetSubscriptions();

        // stop delivery on the original subscribers
        verify(first).stopAsync();
        verify(second).stopAsync();
        // backlog discarded...
        var projectSubscription = ProjectSubscriptionName.of("project", "subscription").toString();
        var rawProjectSubscription = ProjectSubscriptionName.of("project", "raw-subscription").toString();
        verify(admin, atLeastOnce()).deleteSubscription(eq(projectSubscription));
        verify(admin, atLeastOnce()).deleteSubscription(eq(rawProjectSubscription));
        // ...recreated empty...
        verify(admin, atLeastOnce()).createSubscription(any(Subscription.Builder.class));
        // ...and re-subscribed (one initial subscribe per subscription + one reset per subscription)
        verify(subscriberTemplate, times(2)).subscribe(eq(projectSubscription), any());
        verify(subscriberTemplate, times(2)).subscribe(eq(rawProjectSubscription), any());
    }

    @Test
    void resetDoesNotWeakenDeadLetterOrRetryProvisioning() {
        var admin = mock(PubSubAdmin.class);
        var subscriberTemplate = mock(PubSubSubscriberTemplate.class);
        when(admin.getTopic(anyString())).thenReturn(Topic.getDefaultInstance());
        when(admin.getSubscription(anyString())).thenReturn(null);
        doReturn(mock(Subscriber.class)).when(subscriberTemplate).subscribe(anyString(), any());
        var util = new PubSubUtil("project", "app", "", 7, 10, 100, 1.5f,
                admin, subscriberTemplate, null, new EventRegistry());

        util.subscribe(new SubscriptionRequest<PubsubMessage>("topic", "subscription", PubsubMessage.class, e -> { }));
        util.resetSubscriptions();

        // subscription recreated with ordering + retry policy + dead-letter policy intact
        verify(admin, atLeastOnce()).createSubscription(org.mockito.ArgumentMatchers.argThat(builder -> {
            var subscription = builder.build();
            return subscription.getEnableMessageOrdering()
                    && subscription.getDeadLetterPolicy().getMaxDeliveryAttempts() == 7
                    && subscription.hasRetryPolicy();
        }));
    }
}
