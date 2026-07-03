package be.appify.prefab.test.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.TypeReference;

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.toKebabCase;
import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.toStoreName;

public class AutoRegisterPrefabStreamsTestDecorator implements PrefabStreams {
    private final PrefabStreams delegate;
    private final EventRegistry eventRegistry;
    private final String appId;

    public AutoRegisterPrefabStreamsTestDecorator(PrefabStreams delegate, EventRegistry eventRegistry, String appId) {
        this.delegate = delegate;
        this.eventRegistry = eventRegistry;
        this.appId = appId;
    }

    @Override
    public <K, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type) {
        eventRegistry.register(appId + "-" + toKebabCase(type.getSimpleName()), type, Event.Serialization.JSON);
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate.from(type), eventRegistry, this, appId);
    }

    @Override
    public <K, M extends Keyed<K>> PrefabStream<K, M> merge(PrefabStream<K, ? extends M> left,
            PrefabStream<K, ? extends M> right) {
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate.merge(left, right), eventRegistry, this, appId);
    }

    @Override
    public <KS, VS extends Keyed<KS>> Store<KS, VS> createStore(TypeReference<VS> type) {
        eventRegistry.register(appId + "-" + toStoreName(type.name()) + "-changelog", type.rawType(), Event.Serialization.JSON);
        return delegate.createStore(type);
    }

    @Override
    public <KS, VS extends Keyed<KS>> Store<KS, VS> sharedStore(String name, TypeReference<VS> type) {
        eventRegistry.register(appId + "-" + toStoreName(name) + "-changelog", type.rawType(), Event.Serialization.JSON);
        return delegate.sharedStore(name, type);
    }
}
