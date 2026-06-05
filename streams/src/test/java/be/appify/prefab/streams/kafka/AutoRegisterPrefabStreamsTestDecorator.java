package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.toKebabCase;

public class AutoRegisterPrefabStreamsTestDecorator implements PrefabStreams {
    private final PrefabStreams delegate;
    private final EventRegistry eventRegistry;

    public AutoRegisterPrefabStreamsTestDecorator(PrefabStreams delegate, EventRegistry eventRegistry) {
        this.delegate = delegate;
        this.eventRegistry = eventRegistry;
    }

    @Override
    public <K extends Key<K>, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type) {
        eventRegistry.register("prefab-streams-test-" + toKebabCase(type.getSimpleName()), type, Event.Serialization.JSON);
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate.from(type), eventRegistry, this);
    }

    @Override
    public <K extends Key<K>, M extends Keyed<K>> PrefabStream<K, M> merge(PrefabStream<K, ? extends M> left,
            PrefabStream<K, ? extends M> right) {
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate.merge(left, right), eventRegistry, this);
    }

    @Override
    public <KS extends Key<KS>, VS extends Keyed<KS>> Store<KS, VS> createStore(Class<VS> type) {
        eventRegistry.register("prefab-streams-test-" + toKebabCase(type.getSimpleName()) + "-changelog", type, Event.Serialization.JSON);
        return delegate.createStore(type);
    }
}
