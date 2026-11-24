package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.pubsub.PubSubMessageDecoder;
import be.appify.prefab.core.pubsub.PubSubSubscriptions;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Subscribes generated Pub/Sub subscribers to the {@link InMemoryEventBus}, replacing the {@code PubSubUtil} that
 * needs a Pub/Sub connection.
 * <p>
 * The topic, the event type and the consumer of the request are honoured, and messages are read with the same
 * {@link PubSubMessageDecoder} as the real subscriber. Subscription names, dead-letter policies, retries and the
 * executor of the request are ignored: a message is delivered once, on the thread that publishes it.
 */
public class InMemoryPubSubSubscriptions implements PubSubSubscriptions {
    private final ObjectProvider<InMemoryEventBus> bus;
    private final PubSubMessageDecoder decoder;

    /**
     * Constructs the subscriptions.
     *
     * @param bus     the bus to subscribe on, resolved lazily so subscribers can be created before the bus
     * @param decoder the decoder shared with the real Pub/Sub subscriber
     */
    public InMemoryPubSubSubscriptions(ObjectProvider<InMemoryEventBus> bus, PubSubMessageDecoder decoder) {
        this.bus = bus;
        this.decoder = decoder;
    }

    @Override
    public <T> void subscribe(SubscriptionRequest<T> request) {
        bus.getObject().subscribe(request.topic(), request.subscription(), message -> {
            if (message.codec() instanceof PubSubMessageCodec) {
                deliver(request, toPubSubMessage(message));
            }
        });
    }

    private <T> void deliver(SubscriptionRequest<T> request, PubsubMessage message) {
        if (request.rawConsumer().isPresent()) {
            request.rawConsumer().orElseThrow().accept(message);
        } else {
            decoder.decode(request, message).ifPresent(request.consumer());
        }
    }

    private static PubsubMessage toPubSubMessage(EncodedMessage message) {
        return PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(message.payload()))
                .putAllAttributes(message.headers())
                .build();
    }
}
