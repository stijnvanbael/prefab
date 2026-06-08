package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PrefabStream<K extends Key<K>, V extends Keyed<K>> {

    PrefabStream<K, V> filter(Predicate<V> predicate);

    <VO extends Keyed<K>> PrefabStream<K, VO> map(Function<V, VO> mapper);

    <VO extends Keyed<K>> PrefabStream<K, VO> flatMap(Function<V, Iterable<VO>> mapper);

    PrefabStream<K, V> branch(Predicate<V> predicate);

    <S extends V> PrefabStream<K, S> branch(Class<S> subtype);

    PrefabStream<K, V> merge(PrefabStream<K, V> other);

    <VO extends Keyed<K>, VR extends Keyed<K>> PrefabStream<K, VR> join(
            PrefabStream<K, VO> other,
            JoinWindow window,
            BiFunction<? super V, ? super VO, ? extends VR> joiner
    );

    <NI, NO, KO extends Key<KO>, VO extends Keyed<KO>> PrefabStream<KO, VO> breakout(
            StreamBreakoutAdapter<K, V, KO, VO, NI, NO> adapter
    );

    <KO extends Key<KO>, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
            StreamProcessor<K, V, KO, VO> processor
    );

    StreamDefinition to(Class<? super V> type);

    StreamDefinition to(String topic);

    PrefabStream<K, V> unwrap();
}
