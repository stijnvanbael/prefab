package be.appify.prefab.core.repository;

import be.appify.prefab.core.service.AggregateEnvelope;

import java.util.Optional;
import java.util.stream.Stream;

public interface Repository<T> {
    AggregateEnvelope<T> save(AggregateEnvelope<T> envelope);

    Optional<AggregateEnvelope<T>> getById(String id);

    Stream<AggregateEnvelope<T>> findAll();

    boolean exists(String id);

    Class<T> aggregateType();
}
