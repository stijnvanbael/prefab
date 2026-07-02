package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.StreamProcessor;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static be.appify.prefab.streams.kafka.KafkaPrefabStreams.toKebabCase;

public class AutoRegisterPrefabStreamTestDecorator<K, V extends Keyed<K>> implements PrefabStream<K, V> {
    private final PrefabStream<K, V> delegate;
    private final EventRegistry eventRegistry;
    private final PrefabStreams streams;
    private final String appId;

    public AutoRegisterPrefabStreamTestDecorator(
            PrefabStream<K, V> delegate,
            EventRegistry eventRegistry,
            PrefabStreams streams,
            String appId
    ) {
        this.delegate = delegate;
        this.eventRegistry = eventRegistry;
        this.streams = streams;
        this.appId = appId;
    }

    private <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> wrap(PrefabStream<KO, VO> delegate) {
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate, eventRegistry, streams, appId);
    }

    private <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> unwrap(PrefabStream<KO, VO> wrapped) {
        if (wrapped instanceof AutoRegisterPrefabStreamTestDecorator<KO, VO> wrapper) {
            return wrapper.delegate;
        }
        return wrapped;
    }

    @Override
    public PrefabStream<K, V> filter(Predicate<V> predicate) {
        return wrap(delegate.filter(predicate));
    }

    @Override
    public <VO extends Keyed<K>> PrefabStream<K, VO> map(Function<V, VO> mapper) {
        return wrap(delegate.map(mapper));
    }

    @Override
    public <VO extends Keyed<K>> PrefabStream<K, VO> flatMap(Function<V, Iterable<VO>> mapper) {
        return wrap(delegate.flatMap(mapper));
    }

    @Override
    public PrefabStream<K, V> branch(Predicate<V> predicate) {
        return wrap(delegate.branch(predicate));
    }

    @Override
    public <S extends V> PrefabStream<K, S> branch(Class<S> subtype) {
        return wrap(delegate.branch(subtype));
    }

    @Override
    public PrefabStream<K, V> merge(PrefabStream<K, V> other) {
        return wrap(delegate.merge(unwrap(other)));
    }

    @Override
    public <VO extends Keyed<K>, VR extends Keyed<K>> PrefabStream<K, VR> join(
            PrefabStream<K, VO> other,
            JoinWindow window,
            BiFunction<? super V, ? super VO, ? extends VR> joiner
    ) {
        return wrap(delegate.join(unwrap(other), window, joiner));
    }

    @Override
    public <NI, NO, KO, VO extends Keyed<KO>> PrefabStream<KO, VO> breakout(
            StreamBreakoutAdapter<K, V, KO, VO, NI, NO> adapter
    ) {
        return wrap(delegate.breakout(adapter));
    }

    @Override
    public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
            StreamProcessor<K, V, KO, VO> processor
    ) {
        processor.initStreams(streams);
        return wrap(delegate.process(processor));
    }

    @Override
    public StreamDefinition to(Class<? super V> type) {
        eventRegistry.register(appId + "-" + toKebabCase(type.getSimpleName()), type, Event.Serialization.JSON);
        return delegate.to(type);
    }

    @Override
    public StreamDefinition to(String topic) {
        return delegate.to(topic);
    }

    @Override
    public PrefabStream<K, V> unwrap() {
        return delegate;
    }

    @Override
    public Class<V> knownValueType() {
        return delegate.knownValueType();
    }
}
