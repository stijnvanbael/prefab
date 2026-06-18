package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class StatefulStreamProcessor<KI extends Key<KI>, VI extends Keyed<KI>, KO extends Key<KO>, VO extends Keyed<KO>> implements StreamProcessor<KI, VI, KO, VO> {
    private Map<TypeReference<?>, Store<?, ?>> stores;
    private final ThreadLocal<StreamProcessorContext<KO, VO>> context = new ThreadLocal<>();
    private final Set<TypeReference<?>> storeTypes;

    protected StatefulStreamProcessor(Class<?>... storeTypes) {
        this.storeTypes = Arrays.stream(storeTypes)
                .map(TypeReference::of)
                .collect(Collectors.toSet());
    }

    protected StatefulStreamProcessor(TypeReference<?>... storeTypes) {
        this.storeTypes = Set.of(storeTypes);
    }

    @Override
    public void initStreams(PrefabStreams streams) {
        if(stores == null) {
            this.stores = storeTypes.stream()
                    .collect(Collectors.toMap(type -> type, type -> createStore(streams, type)));
        }
    }

    @Override
    public Collection<Store<?, ?>> stateStores() {
        return stores.values();
    }

    @SuppressWarnings("unchecked")
    private static <K extends Key<K>, V extends Keyed<K>> Store<K, V> createStore(PrefabStreams streams, TypeReference<?> type) {
        return streams.createStore((TypeReference<V>) type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K extends Key<K>, V extends Keyed<K>> Store<K, V> store(TypeReference<V> type) {
        if (!stores.containsKey(type)) {
            throw new NoSuchElementException("No store found for type: " + type);
        }
        return (Store<K, V>) stores.get(type);
    }

    public void forward(KO key, VO value) {
        forward(new StreamRecord<>(key, value, Instant.now(), Collections.emptyMap()));
    }

    @Override
    public void forward(StreamRecord<KO, VO> streamRecord) {
        var processorContext = context.get();
        if (processorContext == null) {
            throw new IllegalStateException("Processor context is not initialized, please call init() first");
        }
        processorContext.forward(streamRecord);
    }

    @Override
    public void initContext(StreamProcessorContext<KO, VO> context) {
        this.context.set(context);
        stores.values().forEach(store -> store.init(context));
    }
}
