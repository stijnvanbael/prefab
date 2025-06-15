package be.appify.prefab.processor.spring;

import be.appify.prefab.core.repository.Repository;
import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.util.Lazy;

public class SpringDataReference<T> extends AggregateReference.IdOnlyAggregateReference<T, String>
        implements Reference<T> {

    private final Repository<T> repository;
    private final Lazy<AggregateEnvelope<T>> resolved;

    public SpringDataReference(String id, Repository<T> repository) {
        super(id);
        this.repository = repository;
        resolved = Lazy.of(() -> repository.getById(id()).orElseThrow());
    }

    @Override
    @JsonValue
    public String id() {
        return getId();
    }

    @Override
    public boolean exists() {
        return repository.exists(id());
    }

    @Override
    public T resolveReadOnly() {
        return resolved.get().aggregate();
    }
}
