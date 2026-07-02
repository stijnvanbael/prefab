package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import java.util.Collection;

public interface StreamProcessor<KI, VI, KO, VO> {
    void process(StreamRecord<KI, VI> streamRecord);

    Collection<Store<?, ?>> stateStores();

    void forward(StreamRecord<KO, VO> streamRecord);

    default <K, V extends Keyed<K>> Store<K, V> store(Class<V> type) {
        return store(TypeReference.of(type));
    }

    <K, V extends Keyed<K>> Store<K, V> store(TypeReference<V> type);

    void initStreams(PrefabStreams streams);

    void initContext(StreamProcessorContext<KO, VO> context);
}
