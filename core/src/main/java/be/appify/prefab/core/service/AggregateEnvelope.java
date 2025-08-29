package be.appify.prefab.core.service;

import java.util.function.Consumer;
import java.util.function.Function;

public record AggregateEnvelope<T>(
        T aggregate,
        String id,
        int version
) {
    public AggregateEnvelope {
        IdCache.INSTANCE.put(aggregate, id);
    }

    public static <T> AggregateEnvelope<T> createNew(T entity) {
        return new AggregateEnvelope<>(entity, IdCache.INSTANCE.getId(entity), 0);
    }

    public AggregateEnvelope<T> map(Function<T, T> transformation) {
        return new AggregateEnvelope<>(transformation.apply(aggregate), id, version);
    }

    public AggregateEnvelope<T> apply(Consumer<AggregateEnvelope<T>> consumer) {
        consumer.accept(this);
        return this;
    }
}
