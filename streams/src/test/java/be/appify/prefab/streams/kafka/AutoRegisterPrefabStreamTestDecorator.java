package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
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

public class AutoRegisterPrefabStreamTestDecorator<V> implements PrefabStream<V> {
    private final PrefabStream<V> delegate;
    private final EventRegistry eventRegistry;
    private final PrefabStreams streams;

    public AutoRegisterPrefabStreamTestDecorator(PrefabStream<V> delegate, EventRegistry eventRegistry, PrefabStreams streams) {
        this.delegate = delegate;
        this.eventRegistry = eventRegistry;
        this.streams = streams;
    }

    private <VO> PrefabStream<VO> wrap(PrefabStream<VO> delegate) {
        return new AutoRegisterPrefabStreamTestDecorator<>(delegate, eventRegistry, streams);
    }

    private <VO> PrefabStream<VO> unwrap(PrefabStream<VO> wrapped) {
        if(wrapped instanceof AutoRegisterPrefabStreamTestDecorator<VO> wrapper) {
            return wrapper.delegate;
        }
        return wrapped;
    }

    @Override
    public PrefabStream<V> filter(Predicate<V> predicate) {
        return wrap(delegate.filter(predicate));
    }

    @Override
    public <R> PrefabStream<R> map(Function<V, R> mapper) {
        return wrap(delegate.map(mapper));
    }

    @Override
    public <R> PrefabStream<R> flatMap(Function<V, Iterable<R>> mapper) {
        return wrap(delegate.flatMap(mapper));
    }

    @Override
    public PrefabStream<V> branch(Predicate<V> predicate) {
        return wrap(delegate.branch(predicate));
    }

    @Override
    public <S extends V> PrefabStream<S> branch(Class<S> subtype) {
        return wrap(delegate.branch(subtype));
    }

    @Override
    public PrefabStream<V> merge(PrefabStream<? extends V> other) {
        return wrap(delegate.merge(unwrap(other)));
    }

    @Override
    public <VO, VR> PrefabStream<VR> join(PrefabStream<VO> other, JoinWindow window, BiFunction<? super V, ? super VO, ? extends VR> joiner) {
        return wrap(delegate.join(unwrap(other), window, joiner));
    }

    @Override
    public <R, NATIVE_IN, NATIVE_OUT> PrefabStream<R> breakout(StreamBreakoutAdapter<V, R, NATIVE_IN, NATIVE_OUT> adapter) {
        return wrap(delegate.breakout(adapter));
    }

    @Override
    public <VO> PrefabStream<VO> process(StreamProcessor<V, VO> processor) {
        processor.initStreams(streams);
        return wrap(delegate.process(processor));
    }

    @Override
    public StreamDefinition to(Class<? super V> type) {
        eventRegistry.register("prefab-streams-test-" + toKebabCase(type.getSimpleName()), type, Event.Serialization.JSON);
        return delegate.to(type);
    }

    @Override
    public StreamDefinition to(String topic) {
        return delegate.to(topic);
    }
}
