package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
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
    public <V> PrefabStream<V> from(Class<V> type) {
        eventRegistry.register("prefab-streams-test-" + toKebabCase(type.getSimpleName()), type, Event.Serialization.JSON);
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate.from(type), eventRegistry, this);
    }

    @Override
    public <M> PrefabStream<M> merge(PrefabStream<? extends M> left, PrefabStream<? extends M> right) {
        return new  AutoRegisterPrefabStreamTestDecorator<>(delegate.merge(left, right), eventRegistry, this);
    }

    @Override
    public <VS> Store<VS> createStore(Class<VS> type) {
        eventRegistry.register("prefab-streams-test-" + toKebabCase(type.getSimpleName()) + "-changelog", type, Event.Serialization.JSON);
        return delegate.createStore(type);
    }
}
