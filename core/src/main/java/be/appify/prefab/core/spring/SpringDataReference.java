package be.appify.prefab.core.spring;

import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

/**
 * SpringDataReference is an implementation of AggregateReference and Reference
 * that uses a Spring Data CrudRepository to resolve the referenced entity.
 *
 * @param <T> the type of the referenced entity
 */
public class SpringDataReference<T> implements AggregateReference<T, String>, Reference<T> {

    private final String id;

    SpringDataReference(String id) {
        this.id = id;
    }

    @Override
    @JsonValue
    public String id() {
        return getId();
    }

    @Override
    public String getId() {
        return id;
    }
}
