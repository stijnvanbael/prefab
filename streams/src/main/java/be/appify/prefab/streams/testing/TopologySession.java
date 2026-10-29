package be.appify.prefab.streams.testing;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.GlobalStore;
import java.util.Optional;

/**
 * A running instance of the topology declared on a {@link be.appify.prefab.streams.PrefabStreams}, used to feed input
 * and read output.
 */
public interface TopologySession extends AutoCloseable {

    /**
     * Opens the input of the topic that carries the given type.
     *
     * @param type the record type
     * @param <K>  the key type, resolved from the record type
     * @param <V>  the record type
     * @return the input
     */
    <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type);

    /**
     * Opens the input of the topic that carries the given type, for key types that cannot be resolved from the type.
     *
     * @param type    the record type
     * @param keyType the key type
     * @param <K>     the key type
     * @param <V>     the record type
     * @return the input
     */
    <K, V extends Keyed<K>> TopologyInput<K, V> input(Class<V> type, Class<K> keyType);

    /**
     * Opens the output of the topic that carries the given type.
     *
     * @param type the record type
     * @param <K>  the key type, resolved from the record type
     * @param <V>  the record type
     * @return the output
     */
    <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type);

    /**
     * Opens the output of the topic that carries the given type, for key types that cannot be resolved from the type.
     *
     * @param type    the record type
     * @param keyType the key type
     * @param <K>     the key type
     * @param <V>     the record type
     * @return the output
     */
    <K, V extends Keyed<K>> TopologyOutput<K, V> output(Class<V> type, Class<K> keyType);

    /**
     * Opens the output of a specific topic, for types that are written to more than one topic.
     *
     * @param topic the topic name
     * @param type  the record type
     * @param <K>   the key type, resolved from the record type
     * @param <V>   the record type
     * @return the output
     */
    <K, V extends Keyed<K>> TopologyOutput<K, V> output(String topic, Class<V> type);

    /**
     * Opens the output of a named topic without interpreting its values.
     *
     * @param topic the topic name
     * @return the output, with the serialized values
     */
    TopologyOutput<String, byte[]> output(String topic);

    /**
     * Looks up a record in a global store, e.g. to assert on what an updater has written.
     *
     * @param store the global store declared on the topology under test
     * @param key   the key to look up
     * @param <K>   the key type
     * @param <V>   the value type
     * @return the stored value, if any
     */
    <K, V> Optional<V> lookup(GlobalStore<K, V> store, K key);

    /**
     * Describes the structure of the topology under test.
     *
     * @return the inspection
     */
    TopologyInspection inspect();

    /**
     * Gives access to the backend-specific test facility, for assertions that only make sense for one backend.
     *
     * @param type the type of the backend-specific facility, e.g. Kafka's {@code TopologyTestDriver}
     * @param <T>  the type of the facility
     * @return the facility
     * @throws IllegalArgumentException when the backend has no facility of the given type
     */
    <T> T unwrap(Class<T> type);

    @Override
    void close();
}
