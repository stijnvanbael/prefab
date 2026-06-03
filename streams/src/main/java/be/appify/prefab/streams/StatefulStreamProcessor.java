package be.appify.prefab.streams;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class StatefulStreamProcessor<VI, VO> implements StreamProcessor<VI, VO> {
    private Map<Class<?>, Store<?>> stores;
    private final ThreadLocal<StreamProcessorContext<VO>> context = new ThreadLocal<>();
    private final PrefabStreams streams;
    private final Set<Class<?>> storeTypes;

    protected StatefulStreamProcessor(PrefabStreams streams, Class<?>... storeTypes) {
        this.streams = streams;
        this.storeTypes = Set.of(storeTypes);
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

    @Override
    public void forward(StreamRecord<VO> value) {
        var processorContext = context.get();
        if (processorContext == null) {
            throw new IllegalStateException("Processor context is not initialized, please call init() first");
        }
        processorContext.forward(value);
    }

    @Override
    public void init(StreamProcessorContext<VO> context) {
        this.context.set(context);
        this.stores = storeTypes.stream()
                .collect(Collectors.toMap(type -> type, type -> streams.createStore(type, context)));
    }
}
