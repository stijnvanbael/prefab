package be.appify.prefab.streams;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public abstract class StatefulStreamProcessor<VI, VO> implements StreamProcessor<VI, VO> {
    private final Map<Class<?>, Store<?>> stores;

    protected StatefulStreamProcessor(PrefabStreams streams, Class<?>... storeTypes) {
        this.stores = Arrays.stream(storeTypes)
                .collect(Collectors.toMap(type -> type, streams::createStore));
    }

    @Override
    public Collection<Store<?>> stateStores() {
        return stores.values();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Store<V> store(Class<V> type) {
        if(!stores.containsKey(type)){
            throw new NoSuchElementException("No store found for type: " + type.getName());
        }
        return (Store<V>) stores.get(type);
    }


}
