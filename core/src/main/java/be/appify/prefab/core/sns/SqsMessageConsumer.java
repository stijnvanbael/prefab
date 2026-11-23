package be.appify.prefab.core.sns;

/**
 * Consumer of SQS deliveries that controls when the message is deleted from the queue.
 *
 * <p>The message stays on the queue until the consumer calls {@link Acknowledgement#acknowledge()}, so a consumer
 * can commit its own work first. When the consumer throws or never acknowledges, the message becomes visible again
 * after the visibility timeout and, after the configured number of receives, moves to the dead-letter queue.
 *
 * @param <T> the type of the event
 */
@FunctionalInterface
public interface SqsMessageConsumer<T> {

    /**
     * Handles a delivery.
     *
     * @param event the deserialized event, or {@code null} for a tombstone
     * @param metadata the metadata of the delivery
     * @param acknowledgement deletes the message from the queue
     */
    void accept(T event, SqsMessageMetadata metadata, Acknowledgement acknowledgement);

    /** Deletes the delivered message from its queue. */
    @FunctionalInterface
    interface Acknowledgement {
        /** Deletes the message. */
        void acknowledge();
    }
}
