package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class StatefulStreamProcessor<KI extends Key<KI>, VI extends Keyed<KI>, KO extends Key<KO>, VO extends Keyed<KO>> extends ContextualStreamProcessor<KI, VI, KO, VO> {
    private Map<TypeReference<?>, Store<?, ?>> stores;
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
        if (stores == null) {
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

    /**
     * Returns a state store of the specified type. This method is a convenience method that allows you to retrieve a state store by providing the class of the value type. It internally calls the store(TypeReference) method.
     *
     * @param <K> The type of the key for the state store.
     * @param <V> The type of the value for the state store.
     * @param type The class of the value type for the state store.
     * @return The state store of the specified type.
     */
    protected <K extends Key<K>, V extends Keyed<K>> Store<K, V> store(Class<V> type) {
        return store(TypeReference.of(type));
    }

    /**
     * Returns a state store of the specified type. Implementations should define how to retrieve the state store based on the provided TypeReference.
     *
     * @param <K> The type of the key for the state store.
     * @param <V> The type of the value for the state store.
     * @param type The TypeReference of the value type for the state store.
     * @return The state store of the specified type.
     */
    @SuppressWarnings("unchecked")
    protected <K extends Key<K>, V extends Keyed<K>> Store<K, V> store(TypeReference<V> type) {
        if (!stores.containsKey(type)) {
            throw new NoSuchElementException("No store found for type: " + type);
        }
        return (Store<K, V>) stores.get(type);
    }

    @Override
    public void initContext(StreamProcessorContext<KO, VO> context) {
        super.initContext(context);
        stores.values().forEach(store -> store.init(context));
    }
}
