package be.appify.prefab.streams.engine;

/**
 * Sink SPI for a broker adapter.
 */
@FunctionalInterface
public interface StreamSink {
    /**
     * Publishes an outbox message to a topic.
     *
     * @param topic destination topic
     * @param message message to publish
     */
    void publish(String topic, StreamMessage message);
}
