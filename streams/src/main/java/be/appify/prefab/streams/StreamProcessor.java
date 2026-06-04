package be.appify.prefab.streams;

import java.util.Collection;

public interface StreamProcessor<VI, VO> {
    void process(StreamRecord<VI> streamRecord);

    Collection<Store<?>> stateStores();

    void forward(StreamRecord<VO> streamRecord);

    <V> Store<V> store(Class<V> type);

    void initStreams(PrefabStreams streams);

    void initContext(StreamProcessorContext<VO> context);
}
