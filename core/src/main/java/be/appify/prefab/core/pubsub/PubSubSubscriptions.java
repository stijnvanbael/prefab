package be.appify.prefab.core.pubsub;

/**
 * Subscribes consumers to Pub/Sub topics. Generated subscribers depend on this interface rather than on
 * {@link PubSubUtil}, so another transport, such as the in-memory bus of {@code prefab-test}, can replace the real one.
 */
public interface PubSubSubscriptions {

    /**
     * Subscribes to a Pub/Sub topic using the provided subscription request.
     *
     * @param request the subscribe request containing subscription details
     * @param <T>     the type of the messages
     */
    <T> void subscribe(SubscriptionRequest<T> request);
}
