package be.appify.prefab.test.inmemory.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * The aggregates of all in-memory repositories. Adds {@code @Version} checking with
 * an incrementing version, under a lock per key space.
 */
public final class InMemoryStore {
    private final Map<String, Map<Object, Object>> keySpaces = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final AggregateKeySpaceResolver resolver;
    private final AggregateProperties properties;

    /**
     * Constructs a new InMemoryStore.
     *
     * @param resolver
     *         resolves the key space of an aggregate type
     * @param properties
     *         reads and rewrites aggregate properties
     */
    public InMemoryStore(AggregateKeySpaceResolver resolver, AggregateProperties properties) {
        this.resolver = resolver;
        this.properties = properties;
    }

    /**
     * The property access used by this store.
     *
     * @return the property access
     */
    public AggregateProperties properties() {
        return properties;
    }

    /**
     * Saves an aggregate. A new aggregate is inserted; for a versioned aggregate that is not new the stored version
     * must equal the given one, and the stored copy gets the next version.
     *
     * @param entity
     *         the aggregate
     * @param <T>
     *         the aggregate type
     * @return the saved aggregate, with its new version
     * @throws OptimisticLockingFailureException
     *         if the aggregate was changed or deleted since it was read
     * @throws DuplicateKeyException
     *         if a new aggregate has the identifier of a stored one
     */
    public <T> T save(T entity) {
        var id = idOf(entity);
        var keySpace = resolver.resolveKeySpace(entity.getClass());
        return locked(keySpace, () -> {
            var saved = properties.versionOf(entity)
                    .map(version -> versioned(entity, id, keySpace, version))
                    .orElse(entity);
            mapOf(keySpace).put(id, saved);
            return saved;
        });
    }

    private <T> T versioned(T entity, Object id, String keySpace, AggregateProperties.Version version) {
        if (version.isNew()) {
            if (mapOf(keySpace).containsKey(id)) {
                throw new DuplicateKeyException("%s with id %s already exists".formatted(
                        entity.getClass().getSimpleName(), id));
            }
            return properties.withVersion(entity, version.initial());
        }
        assertStoredVersion(entity, id, keySpace, version);
        return properties.withVersion(entity, version.value() + 1);
    }

    private void assertStoredVersion(Object entity, Object id, String keySpace, AggregateProperties.Version version) {
        var stored = Optional.ofNullable(mapOf(keySpace).get(id))
                .flatMap(properties::versionOf)
                .filter(AggregateProperties.Version::assigned);
        if (stored.map(AggregateProperties.Version::value).filter(value -> value == version.value()).isEmpty()) {
            throw new OptimisticLockingFailureException(
                    "%s with id %s and version %d was changed or deleted by another transaction".formatted(
                            entity.getClass().getSimpleName(), id, version.value()));
        }
    }

    /**
     * Deletes an aggregate. A versioned aggregate must still have the stored version.
     *
     * @param entity
     *         the aggregate
     * @throws OptimisticLockingFailureException
     *         if the aggregate was changed or deleted since it was read
     */
    public void delete(Object entity) {
        var id = idOf(entity);
        var keySpace = resolver.resolveKeySpace(entity.getClass());
        locked(keySpace, () -> {
            properties.versionOf(entity)
                    .filter(version -> !version.isNew())
                    .ifPresent(version -> assertStoredVersion(entity, id, keySpace, version));
            mapOf(keySpace).remove(id);
        });
    }

    /**
     * Deletes the aggregate with an identifier, if there is one.
     *
     * @param id
     *         the identifier
     * @param type
     *         the aggregate type of the repository
     */
    public void deleteById(Object id, Class<?> type) {
        var keySpace = resolver.resolveKeySpace(type);
        locked(keySpace, () -> mapOf(keySpace).remove(id));
    }

    /**
     * Deletes all aggregates of a type.
     *
     * @param type
     *         the aggregate type of the repository
     */
    public void deleteAll(Class<?> type) {
        var keySpace = resolver.resolveKeySpace(type);
        locked(keySpace, () -> mapOf(keySpace).clear());
    }

    /**
     * Finds an aggregate by identifier.
     *
     * @param id
     *         the identifier
     * @param type
     *         the aggregate type of the repository
     * @param <T>
     *         the aggregate type
     * @return the aggregate, if present
     */
    public <T> Optional<T> findById(Object id, Class<T> type) {
        return Optional.ofNullable(mapOf(resolver.resolveKeySpace(type)).get(id)).filter(type::isInstance).map(type::cast);
    }

    /**
     * All aggregates of a type, in no particular order.
     *
     * @param type
     *         the aggregate type of the repository
     * @param <T>
     *         the aggregate type
     * @return a snapshot of the aggregates
     */
    public <T> List<T> findAll(Class<T> type) {
        var all = new ArrayList<T>();
        mapOf(resolver.resolveKeySpace(type)).values().stream().filter(type::isInstance).map(type::cast)
                .forEach(all::add);
        return all;
    }

    /**
     * Counts the aggregates of a type.
     *
     * @param type
     *         the aggregate type of the repository
     * @return the number of aggregates
     */
    public long count(Class<?> type) {
        return findAll(type).size();
    }

    private Map<Object, Object> mapOf(String keySpace) {
        return keySpaces.computeIfAbsent(keySpace, ignored -> new ConcurrentHashMap<>());
    }

    private Object idOf(Object entity) {
        return properties.idOf(entity).orElseThrow(() -> new IllegalArgumentException(
                "%s has no id, so it cannot be stored".formatted(entity.getClass().getSimpleName())));
    }

    private void locked(String keySpace, Runnable action) {
        locked(keySpace, () -> {
            action.run();
            return action;
        });
    }

    private <R> R locked(String keySpace, Supplier<R> action) {
        var lock = locks.computeIfAbsent(keySpace, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
