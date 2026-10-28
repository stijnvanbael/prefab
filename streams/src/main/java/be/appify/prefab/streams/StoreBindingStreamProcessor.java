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
    private final Collection<ReadableStore<?, ?>> explicitStores;
    private Collection<Store<?, ?>> localStores;
    private Collection<GlobalStore<?, ?>> globalStores;

    StoreBindingStreamProcessor(StreamProcessor<KI, VI, KO, VO> delegate, Collection<ReadableStore<?, ?>> explicitStores) {
        this.delegate = Objects.requireNonNull(delegate, "processor must not be null");
        this.explicitStores = List.copyOf(explicitStores);
    }

    @Override
    public void process(StreamRecord<KI, VI> streamRecord) {
        delegate.process(streamRecord);
    }

    @Override
    public Collection<Store<?, ?>> stateStores() {
        requireInitialized();
        return localStores;
    }

    @Override
    public Collection<GlobalStore<?, ?>> globalStores() {
        requireInitialized();
        return globalStores;
    }

    @Override
    public void initStreams(PrefabStreams streams) {
        delegate.initStreams(streams);
        var merged = new LinkedHashMap<String, ReadableStore<?, ?>>();
        addStores(merged, delegate.stateStores(), "processor");
        addStores(merged, delegate.globalStores(), "processor");
        addStores(merged, explicitStores, "explicit");
        this.localStores = merged.values().stream()
                .<Store<?, ?>>mapMulti((store, sink) -> {
                    if (store instanceof Store<?, ?> local) {
                        sink.accept(local);
                    }
                })
                .toList();
        this.globalStores = merged.values().stream()
                .<GlobalStore<?, ?>>mapMulti((store, sink) -> {
                    if (store instanceof GlobalStore<?, ?> global) {
                        sink.accept(global);
                    }
                })
                .toList();
    }
    @Override
    public void initContext(StreamProcessorContext<KO, VO> context) {
        delegate.initContext(context);
        localStores.forEach(store -> store.init(context));
        globalStores.forEach(store -> store.init(context));
    }

    private void requireInitialized() {
        if (localStores == null) {
            throw new IllegalStateException("Store bindings are not initialized; initStreams must run before stateStores()");
        }
    }

    private static void addStores(
            Map<String, ReadableStore<?, ?>> storesByName,
            Collection<? extends ReadableStore<?, ?>> stores,
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
                        "Duplicate store binding '%s' uses different store instances; reuse a single declaration"
                                .formatted(name)
                );
            }
        }
    }
}