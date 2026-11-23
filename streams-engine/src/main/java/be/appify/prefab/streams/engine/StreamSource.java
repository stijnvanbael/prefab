package be.appify.prefab.streams.engine;

/**
 * Source SPI for a broker adapter. Implementations deliver one message at a time and invoke acknowledgement only
 * after the engine has committed processing.
 */
public interface StreamSource {
    /** Whether deliveries from the topic preserve per-key order. */
    default boolean ordered(String topic) {
        return false;
    }

    /**
     * Subscribes to a logical topic.
     *
     * @param topic source topic
     * @param handler message handler
     * @return a handle that stops this subscription
     */
    Subscription subscribe(String topic, MessageHandler handler);

    /** Callback invoked for a single broker delivery. */
    @FunctionalInterface
    interface MessageHandler {
        void onMessage(StreamMessage message, Acknowledgement acknowledgement);
    }

    /** Broker acknowledgement action. */
    @FunctionalInterface
    interface Acknowledgement {
        void acknowledge();
    }

    /** Handle for a running subscription. */
    @FunctionalInterface
    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}
