package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

/**
 * Adapter SPI for injecting backend-native stream fragments in a Prefab stream pipeline.
 *
 * @param <KI> current Prefab key type
 * @param <VI> current Prefab value type
 * @param <KO> resulting Prefab key type
 * @param <VO> resulting Prefab value type
 * @param <NATIVE_IN> backend-native input stream type
 * @param <NATIVE_OUT> backend-native output stream type
 */
public interface StreamBreakoutAdapter<
        KI extends Key<KI>,
        VI extends Keyed<KI>,
        KO extends Key<KO>,
        VO extends Keyed<KO>,
        NATIVE_IN,
        NATIVE_OUT
        > {

    /** Backend this adapter targets. */
    StreamBackend backend();

    /** Runtime type expected as native input stream. */
    Class<NATIVE_IN> nativeInputType();

    /** Applies the native stream fragment. */
    NATIVE_OUT apply(NATIVE_IN nativeStream);
}
