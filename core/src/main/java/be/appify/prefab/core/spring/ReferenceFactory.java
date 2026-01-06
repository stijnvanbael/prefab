package be.appify.prefab.core.spring;

import be.appify.prefab.core.service.Reference;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Factory for creating Reference objects for entities managed by Spring Data repositories.
 */
@Component
public class ReferenceFactory {
    private final Map<Class<?>, CrudRepository<?, String>> crudRepositories = new HashMap<>();

    ReferenceFactory() {
    }

    /**
     * Handles the ApplicationReadyEvent to populate the map of entity types to their corresponding CrudRepository
     * instances.
     *
     * @param event
     *         the ApplicationReadyEvent
     */
    @SuppressWarnings("unchecked")
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        event.getApplicationContext().getBeansOfType(CrudRepository.class).values().forEach(repository ->
                Stream.of(repository.getClass().getInterfaces()[0].getGenericInterfaces())
                        .filter(ReferenceFactory::isCrudRepository)
                        .map(type -> (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0])
                        .forEach(entityType -> crudRepositories.put(entityType, repository)));
    }

    private static boolean isCrudRepository(Type type) {
        return type instanceof ParameterizedType parameterizedType &&
                parameterizedType.getRawType() instanceof Class<?> rawClass &&
                CrudRepository.class.isAssignableFrom(rawClass);
    }

    /**
     * Creates a Reference object for the specified entity class and identifier.
     *
     * @param clazz
     *         the entity class
     * @param id
     *         the identifier of the entity
     * @param <T>
     *         the type of the entity
     * @return a Reference to the entity, or null if the id is null
     * @throws IllegalArgumentException
     *         if no repository is found for the specified class
     */
    @SuppressWarnings("unchecked")
    public <T> Reference<T> referenceTo(Class<T> clazz, String id) {
        if (id == null) {
            return null;
        }
        var repository = crudRepositories.get(clazz);
        if (repository == null) {
            throw new IllegalArgumentException("No repository found for class " + clazz.getName());
        }
        return new SpringDataReference<>(id, (CrudRepository<T, String>) repository);
    }
}
