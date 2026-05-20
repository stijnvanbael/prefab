package be.appify.prefab.streams;

/**
 * Adapter SPI for injecting backend-native stream fragments in a Prefab stream pipeline.
 *
 * @param <V> current Prefab value type
 * @param <R> resulting Prefab value type
 * @param <NATIVE_IN> backend-native input stream type
 * @param <NATIVE_OUT> backend-native output stream type
 */
public interface StreamBreakoutAdapter<V, R, NATIVE_IN, NATIVE_OUT> {

    /** Backend this adapter targets. */
    StreamBackend backend();

    /** Runtime type expected as native input stream. */
    Class<NATIVE_IN> nativeInputType();

    /** Applies the native stream fragment. */
    NATIVE_OUT apply(NATIVE_IN nativeStream);
}

