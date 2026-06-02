package be.appify.prefab.streams;

import java.util.Collection;

public interface StreamProcessor<VI, VO> {
    void process(StreamRecord<VI> value);

    Collection<Store<?>> stateStores();

    void forward(StreamRecord<VO> value);

    <V> Store<V> store(Class<V> type);
}
