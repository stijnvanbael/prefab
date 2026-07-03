package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decorates a processor with additional store bindings that should be available at runtime.
 */
final class StoreBindingStreamProcessor<KI, VI extends Keyed<KI>, KO, VO extends Keyed<KO>>
        implements StreamProcessor<KI, VI, KO, VO> {
    private final StreamProcessor<KI, VI, KO, VO> delegate;
    private final Collection<Store<?, ?>> explicitStores;
    private List<Store<?, ?>> stores;

    StoreBindingStreamProcessor(StreamProcessor<KI, VI, KO, VO> delegate, Collection<Store<?, ?>> explicitStores) {
        this.delegate = Objects.requireNonNull(delegate, "processor must not be null");
        this.explicitStores = List.copyOf(explicitStores);
    }

    @Override
    public void process(StreamRecord<KI, VI> streamRecord) {
        delegate.process(streamRecord);
    }

    @Override
    public Collection<Store<?, ?>> stateStores() {
        if (stores == null) {
            throw new IllegalStateException("Store bindings are not initialized; initStreams must run before stateStores()");
        }
        return stores;
    }

    @Override
    public void initStreams(PrefabStreams streams) {
        delegate.initStreams(streams);
        this.stores = mergeStores(delegate.stateStores(), explicitStores);
    }

    @Override
    public void initContext(StreamProcessorContext<KO, VO> context) {
        delegate.initContext(context);
        stores.forEach(store -> store.init(context));
    }

    private static List<Store<?, ?>> mergeStores(
            Collection<Store<?, ?>> processorStores,
            Collection<Store<?, ?>> explicitStores
    ) {
        var storesByName = new LinkedHashMap<String, Store<?, ?>>();
        addStores(storesByName, processorStores, "processor");
        addStores(storesByName, explicitStores, "explicit");
        return List.copyOf(storesByName.values());
    }

    private static void addStores(
            Map<String, Store<?, ?>> storesByName,
            Collection<Store<?, ?>> stores,
            String origin
    ) {
        for (var store : stores) {
            if (store == null) {
                throw new IllegalArgumentException("%s store binding must not be null".formatted(origin));
            }
            var name = store.name();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("%s store binding must have a non-blank name".formatted(origin));
            }
            var existing = storesByName.putIfAbsent(name, store);
            if (existing != null && existing != store) {
                throw new IllegalArgumentException(
                        "Duplicate store binding '%s' uses different store instances; reuse a single shared declaration"
                                .formatted(name)
                );
            }
        }
    }
}



