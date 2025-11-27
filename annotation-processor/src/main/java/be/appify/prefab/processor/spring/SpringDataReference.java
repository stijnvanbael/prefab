package be.appify.prefab.processor.spring;

import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Lazy;

public class SpringDataReference<T> implements AggregateReference<T, String>, Reference<T> {

    private final CrudRepository<T, String> repository;
    private final Lazy<T> resolved;
    private final String id;

    public SpringDataReference(String id, CrudRepository<T, String> repository) {
        this.id = id;
        this.repository = repository;
        resolved = Lazy.of(() -> repository.findById(id()).orElseThrow());
    }

    @Override
    @JsonValue
    public String id() {
        return getId();
    }

    @Override
    public boolean exists() {
        return repository.existsById(id());
    }

    @Override
    public T resolveReadOnly() {
        return resolved.get();
    }

    @Override
    public String getId() {
        return id;
    }
}
