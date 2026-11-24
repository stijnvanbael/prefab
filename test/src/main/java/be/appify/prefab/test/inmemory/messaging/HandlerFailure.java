package be.appify.prefab.test.inmemory.messaging;

/**
 * A subscriber that threw while handling a message.
 *
 * @param topic      the topic of the message
 * @param subscriber the name of the failing subscriber
 * @param cause      the exception thrown by the subscriber
 */
public record HandlerFailure(String topic, String subscriber, Throwable cause) {
}
