package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

import java.util.Collection;
import java.util.Collections;

/**
 * A StreamProcessor is responsible for processing incoming stream records and producing outgoing stream records.
 * It can also access state stores to maintain state across multiple records.
 *
 * @param <KI> The type of the key for the input records.
 * @param <VI> The type of the value for the input records.
 * @param <KO> The type of the key for the output records.
 * @param <VO> The type of the value for the output records.
 */
public interface StreamProcessor<KI extends Key<KI>, VI extends Keyed<KI>, KO extends Key<KO>, VO extends Keyed<KO>> {
    /**
     * Processes an incoming stream record. Implementations should define the logic for handling the input record and producing any necessary output records.
     *
     * @param streamRecord The incoming stream record to be processed.
     */
    void process(StreamRecord<KI, VI> streamRecord);

    /**
     * Returns a collection of state stores that this processor uses. By default, it returns an empty list, indicating that the processor does not use any state stores.
     * Implementations can override this method to provide the specific state stores they require.
     *
     * @return A collection of state stores used by this processor.
     */
    default Collection<Store<?, ?>> stateStores() {
        return Collections.emptyList();
    }

    /**
     * Initializes the streams for this processor. This method is called during the initialization phase of the stream processing topology. Implementations can override this method to perform any necessary setup or configuration for the streams.
     *
     * @param streams The PrefabStreams instance that provides access to the stream processing environment.
     */
    default void initStreams(PrefabStreams streams) {
    }

    /**
     * Initializes the context for this processor. This method is called during the initialization phase of the stream processing topology. Implementations can override this method to perform any necessary setup or configuration for the processor context.
     *
     * @param context The StreamProcessorContext that provides access to the processor's context, including state stores and forwarding capabilities.
     */
    void initContext(StreamProcessorContext<KO, VO> context);
}
