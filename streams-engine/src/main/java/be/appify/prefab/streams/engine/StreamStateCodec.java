package be.appify.prefab.streams.engine;

/**
 * Converts state values and outbox messages to and from the bytes stored by a database-backed repository.
 *
 * <p>Implementations must be able to restore the exact Java type that was written, because state values are
 * handed back to operators without type information.
 */
public interface StreamStateCodec {
    /**
     * Encodes a state value.
     *
     * @param value the value, possibly {@code null}
     * @return the stored bytes
     */
    byte[] encodeValue(Object value);

    /**
     * Decodes a state value written by {@link #encodeValue(Object)}.
     *
     * @param bytes the stored bytes
     * @return the value, or {@code null} if a {@code null} value was written
     */
    Object decodeValue(byte[] bytes);

    /**
     * Encodes an output message for the outbox.
     *
     * @param message the message
     * @return the stored bytes
     */
    byte[] encodeMessage(StreamMessage message);

    /**
     * Decodes an outbox message.
     *
     * @param bytes the stored bytes
     * @return the message
     */
    StreamMessage decodeMessage(byte[] bytes);

    /**
     * Encodes a state key using the shape-based key encoding shared with the Kafka backend.
     *
     * @param key the key
     * @return the encoded key
     */
    byte[] encodeKey(Object key);
}
