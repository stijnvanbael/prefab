package be.appify.prefab.streams.testing;

import be.appify.prefab.core.domain.Keyed;
import java.time.Instant;

/**
 * Feeds records into the input topic of a topology under test.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface TopologyInput<K, V extends Keyed<K>> {

    /**
     * Sends a record, using the key of the value and the current time as timestamp.
     *
     * @param value the record value
     * @return this input, to chain further records
     */
    default TopologyInput<K, V> send(V value) {
        return send(value.key(), value);
    }

    /**
     * Sends a record, using the key of the value.
     *
     * @param value     the record value
     * @param timestamp the record timestamp
     * @return this input, to chain further records
     */
    default TopologyInput<K, V> send(V value, Instant timestamp) {
        return send(value.key(), value, timestamp);
    }

    /**
     * Sends a record with an explicit key.
     *
     * @param key   the record key
     * @param value the record value
     * @return this input, to chain further records
     */
    TopologyInput<K, V> send(K key, V value);

    /**
     * Sends a record with an explicit key and timestamp.
     *
     * @param key       the record key
     * @param value     the record value
     * @param timestamp the record timestamp
     * @return this input, to chain further records
     */
    TopologyInput<K, V> send(K key, V value, Instant timestamp);

    /**
     * Sends a tombstone, a record without a value.
     *
     * @param key the record key
     * @return this input, to chain further records
     */
    TopologyInput<K, V> sendTombstone(K key);

    /**
     * Sends a record that already carries a message ID, as if it was published by a tracing stream.
     *
     * @param value     the record value, its key is used
     * @param messageId the ID of the message
     * @return this input, to chain further records
     */
    TopologyInput<K, V> sendWithMessageId(V value, String messageId);
}
