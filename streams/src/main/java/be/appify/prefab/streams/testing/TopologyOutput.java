package be.appify.prefab.streams.testing;

import java.util.List;
import java.util.Optional;

/**
 * Reads the records a topology under test has written to an output topic.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface TopologyOutput<K, V> {

    /**
     * Reads and consumes all values that are available.
     *
     * @return the values, in order of arrival; empty when nothing was written
     */
    List<V> values();

    /**
     * Reads and consumes the next value.
     *
     * @return the next value
     * @throws java.util.NoSuchElementException when nothing was written
     */
    V value();

    /**
     * Reads and consumes all keys that are available together with their values.
     *
     * @return the records, in order of arrival
     */
    List<Record<K, V>> records();

    /**
     * Reads and consumes all records that are available, including their message and correlation IDs.
     *
     * @return the messages, in order of arrival
     */
    List<Message<K, V>> messages();

    /**
     * Whether no more records are available.
     *
     * @return true when there is nothing left to read
     */
    boolean isEmpty();

    /**
     * A record read from an output topic together with its tracing information.
     *
     * @param key            the record key
     * @param value          the record value
     * @param messageId      the ID of the message, present when tracing is enabled
     * @param correlationIds the IDs of the messages this message derives from; empty when it has no origin message
     * @param <K>            the key type
     * @param <V>            the value type
     */
    record Message<K, V>(K key, V value, Optional<String> messageId, List<String> correlationIds) {
    }

    /**
     * A key-value pair read from an output topic.
     *
     * @param key   the record key
     * @param value the record value
     * @param <K>   the key type
     * @param <V>   the value type
     */
    record Record<K, V>(K key, V value) {
    }
}
