package be.appify.prefab.core.sns;

import java.util.Optional;

/**
 * Reads the event a subscription is interested in from the body of a received SQS message, which is an SNS
 * notification envelope holding the payload, the subject and the message attributes stamped by
 * {@link SnsMessageEncoder}.
 * <p>
 * The real subscriber and the in-memory bus of {@code prefab-test} both use this class.
 */
public class SqsMessageDecoder {
    private final SqsDeserializer deserializer;

    /**
     * Constructs a decoder.
     *
     * @param deserializer the deserializer for the notification envelope
     */
    public SqsMessageDecoder(SqsDeserializer deserializer) {
        this.deserializer = deserializer;
    }

    /**
     * Decodes a message for a subscription.
     *
     * @param request the subscription that receives the event
     * @param body    the body of the received message
     * @param <T>     the event type the subscription expects
     * @return the event, or empty when the message belongs to another concrete binding
     * @throws be.appify.prefab.core.kafka.UnknownEventTypeException if the event type is not registered
     */
    public <T> Optional<T> decode(SqsSubscriptionRequest<T> request, String body) {
        if (request.eventType().isPresent()) {
            return deserializer.deserializeMatching(request.topic(), body, request.eventType().orElseThrow());
        }
        return Optional.of(deserializer.deserialize(request.topic(), body, request.type()));
    }
}
