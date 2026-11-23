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

    /**
     * Subscribes to a named source binding. The name is also the inbox namespace and must be stable across instances.
     * Adapters that need binding or subscription metadata should override this method.
     *
     * @param source source identity and event binding
     * @param handler message handler
     * @return a handle that stops this subscription
     */
    default Subscription subscribe(SourceBinding source, MessageHandler handler) {
        return subscribe(source.topic(), handler);
    }

    /**
     * Describes one logical source or global-store subscription.
     *
     * @param name stable source identity
     * @param topic source topic
     * @param valueType source event class
     * @param eventTypeId concrete generic event identity, or null for a class binding
     */
    record SourceBinding(String name, String topic, Class<?> valueType, String eventTypeId) {
    }

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
