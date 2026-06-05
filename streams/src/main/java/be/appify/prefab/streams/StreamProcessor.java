package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import java.util.Collection;

public interface StreamProcessor<KI, VI, KO, VO> {
    void process(StreamRecord<KI, VI> streamRecord);

    Collection<Store<?, ?>> stateStores();

    void forward(StreamRecord<KO, VO> streamRecord);

    <K extends Key<K>, V extends Keyed<K>> Store<K, V> store(Class<V> type);

    void initStreams(PrefabStreams streams);

    void initContext(StreamProcessorContext<KO, VO> context);
}
