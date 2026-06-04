package be.appify.prefab.streams;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class StatefulStreamProcessor<VI, VO> implements StreamProcessor<VI, VO> {
    private Map<Class<?>, Store<?>> stores;
    private final ThreadLocal<StreamProcessorContext<VO>> context = new ThreadLocal<>();
    private final Set<Class<?>> storeTypes;

    protected StatefulStreamProcessor(Class<?>... storeTypes) {
        this.storeTypes = Set.of(storeTypes);
    }

    @Override
    public void initStreams(PrefabStreams streams) {
        if(stores == null) {
            this.stores = storeTypes.stream()
                    .collect(Collectors.toMap(type -> type, streams::createStore));
        }
    }

    @Override
    public Collection<Store<?>> stateStores() {
        return stores.values();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Store<V> store(Class<V> type) {
        if (!stores.containsKey(type)) {
            throw new NoSuchElementException("No store found for type: " + type.getName());
        }
        return (Store<V>) stores.get(type);
    }

    public void forward(String key, VO value) {
        forward(new StreamRecord<>(key, value, Instant.now(), Collections.emptyMap()));
    }

    @Override
    public void forward(StreamRecord<VO> streamRecord) {
        var processorContext = context.get();
        if (processorContext == null) {
            throw new IllegalStateException("Processor context is not initialized, please call init() first");
        }
        processorContext.forward(streamRecord);
    }

    @Override
    public void initContext(StreamProcessorContext<VO> context) {
        this.context.set(context);
        stores.values().forEach(store -> store.init(context));
    }
}
