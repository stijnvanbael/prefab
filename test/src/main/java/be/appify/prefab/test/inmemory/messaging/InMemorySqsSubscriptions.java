package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.sns.SqsMessageDecoder;
import be.appify.prefab.core.sns.SqsSubscription;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsSubscriptions;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Subscribes generated SQS subscribers to the {@link InMemoryEventBus}, replacing the {@code SqsUtil} that needs
 * SNS and SQS clients.
 * <p>
 * The topic, the event type and the consumer of the request are honoured, and notifications are read with the same
 * {@link SqsMessageDecoder} as the real subscriber. Queue names, dead-letter queues, retries and the executor of the
 * request are ignored: a message is delivered once, on the thread that publishes it.
 */
public class InMemorySqsSubscriptions implements SqsSubscriptions {
    private final ObjectProvider<InMemoryEventBus> bus;
    private final SqsMessageDecoder decoder;

    /**
     * Constructs the subscriptions.
     *
     * @param bus     the bus to subscribe on, resolved lazily so subscribers can be created before the bus
     * @param decoder the decoder shared with the real SQS subscriber
     */
    public InMemorySqsSubscriptions(ObjectProvider<InMemoryEventBus> bus, SqsMessageDecoder decoder) {
        this.bus = bus;
        this.decoder = decoder;
    }

    @Override
    public <T> SqsSubscription subscribe(SqsSubscriptionRequest<T> request) {
        if (request.messageConsumer().isPresent()) {
            throw new UnsupportedOperationException(("SQS subscription on topic [%s] needs message metadata, which "
                    + "only Prefab Streams uses; Prefab Streams does not run on the in-memory bus. "
                    + "Use @IntegrationTest(containers = true) for this test.").formatted(request.topic()));
        }
        var subscription = bus.getObject().subscribe(request.topic(), request.queueName(), message -> {
            if (message.codec() instanceof SnsMessageCodec) {
                var body = new String(message.payload(), StandardCharsets.UTF_8);
                decoder.decode(request, body).ifPresent(request.consumer());
            }
        });
        return subscription::close;
    }
}
