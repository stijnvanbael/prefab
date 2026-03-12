package be.appify.prefab.core.spring;

import be.appify.prefab.core.service.Reference;
import org.springframework.stereotype.Component;

/**
 * Factory for creating Reference objects for entities managed by Spring Data repositories.
 */
@Component
public class ReferenceFactory {
    ReferenceFactory() {
    }
    /**
     * Creates a Reference object for the specified entity class and identifier.
     *
     * @param id
     *         the identifier of the entity
     * @param <T>
     *         the type of the entity
     * @return a Reference to the entity, or null if the id is null
     */
    public <T> Reference<T> referenceTo(String id) {
        if (id == null) {
            return null;
        }
        return new SpringDataReference<>(id);
    }
}
