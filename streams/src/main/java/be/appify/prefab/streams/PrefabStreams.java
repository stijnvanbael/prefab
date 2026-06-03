package be.appify.prefab.streams;

/**
 * Entry point for the Prefab streams DSL.
 */
public interface PrefabStreams {
    /**
     * Starts a stream definition from the topic mapped to the provided event type.
     *
     * @param type event class registered for a Kafka topic
     * @param <V>  record value type
     * @return source stream builder for the given event type
     */
    <V> PrefabStream<V> from(Class<V> type);

    /**
     * Merges two streams into a stream typed to their declared common supertype.
     *
     * @param left  left stream to merge
     * @param right right stream to merge
     * @param <M>   merged value type
     * @return merged stream containing records from both inputs as {@code M}
     */
    <M> PrefabStream<M> merge(PrefabStream<? extends M> left, PrefabStream<? extends M> right);

    /**
     * Creates a new state store of the given type. The returned store is registered in the stream
     * processing topology and can be accessed from {@link StreamProcessor#store(Class)}.
     *
     * @param type store value type
     * @param <VS>  store value type
     * @return new state store of the given type
     */
    <VS> Store<VS> createStore(Class<VS> type, StreamProcessorContext<?> context);
}
