package be.appify.prefab.core.spring;

import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Lazy;

/**
 * SpringDataReference is an implementation of AggregateReference and Reference
 * that uses a Spring Data CrudRepository to resolve the referenced entity.
 *
 * @param <T> the type of the referenced entity
 */
public class SpringDataReference<T> implements AggregateReference<T, String>, Reference<T> {

    private final CrudRepository<T, String> repository;
    private final Lazy<T> resolved;
    private final String id;


    SpringDataReference(String id, CrudRepository<T, String> repository) {
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
