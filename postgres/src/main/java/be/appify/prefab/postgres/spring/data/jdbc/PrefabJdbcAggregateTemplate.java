package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.Outbox;
import be.appify.prefab.core.outbox.OutboxEntry;
import be.appify.prefab.core.outbox.OutboxRepository;
import be.appify.prefab.core.outbox.PendingEventBuffer;
import be.appify.prefab.core.spring.SpringDomainEventPublisher;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Custom {@link JdbcAggregateTemplate} that avoids unnecessary child-entity delete and re-insert operations when saving
 * an aggregate whose child collections have not changed.
 * <p>
 * Spring Data JDBC's default update strategy always deletes all child-entity rows and re-inserts them on every save,
 * even when nothing in the child collection has been modified. For aggregates with large child collections this
 * generates significant unnecessary database traffic.
 * <p>
 * This implementation intercepts {@link #save(Object)} and, for non-new entities, loads the current state from the
 * database and compares each collection-typed property with the corresponding value in the entity being saved. If a
 * collection is unchanged (according to {@link Objects#equals}), its component type is added to a thread-local skip
 * set that is checked by {@link PrefabDataAccessStrategy} before executing the underlying SQL.
 *
 * <h2>Behaviour with record types</h2>
 * The skip optimisation is applied only to collection properties whose component type is a Java {@code record}.
 * Records implement structural equality ({@code equals} / {@code hashCode}) based on all components, which makes them
 * safe to compare with {@link Objects#equals}. Collections whose component type is not a record are always persisted
 * using the default delete-and-re-insert strategy.
 *
 * <h2>Performance trade-off</h2>
 * For every {@code save} of an existing aggregate that has at least one collection property, this implementation issues
 * an additional {@code SELECT} to load the current state from the database before the comparison. If the collections
 * turn out to be unchanged this extra read is more than offset by the avoided bulk delete and re-insert. However, for
 * aggregates where the child collections <em>always</em> change on every save the extra read is pure overhead; in that
 * scenario the built-in Spring Data JDBC behavior is more efficient.
 */
public class PrefabJdbcAggregateTemplate extends JdbcAggregateTemplate {

    private final RelationalMappingContext context;
    private final @Nullable OutboxRepository outboxRepository;
    private final @Nullable JsonMapper jsonMapper;

    /**
     * Constructs a new PrefabJdbcAggregateTemplate.
     *
     * @param applicationContext
     *         the application context, used for publishing lifecycle events
     * @param context
     *         the relational mapping context
     * @param converter
     *         the JDBC converter
     * @param dataAccessStrategy
     *         the data access strategy (should be a {@link PrefabDataAccessStrategy})
     * @param outboxRepository
     *         the outbox repository for transactional outbox support; may be {@code null}
     * @param jsonMapper
     *         the JSON mapper for serializing domain events; may be {@code null}
     */
    public PrefabJdbcAggregateTemplate(
            ApplicationContext applicationContext,
            RelationalMappingContext context,
            JdbcConverter converter,
            DataAccessStrategy dataAccessStrategy,
            @Nullable OutboxRepository outboxRepository,
            @Nullable JsonMapper jsonMapper
    ) {
        super(applicationContext, context, converter, dataAccessStrategy);
        this.context = context;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T save(T instance) {
        Class<T> type = (Class<T>) instance.getClass();
        RelationalPersistentEntity<T> entity = (RelationalPersistentEntity<T>) context.getRequiredPersistentEntity(type);
        T result = performSave(instance, entity, type);
        drainEventsToOutbox(result, entity);
        return result;
    }

    private <T> T performSave(T instance, RelationalPersistentEntity<T> entity, Class<T> type) {
        if (!entity.isNew(instance) && hasCollectionProperties(entity)) {
            Object id = entity.getIdentifierAccessor(instance).getRequiredIdentifier();
            T current = findCurrentState(id, type);
            if (current != null) {
                Set<Class<?>> skipTypes = findUnchangedChildTypes(entity, current, instance);
                if (!skipTypes.isEmpty()) {
                    PrefabDataAccessStrategy.SKIP_CHILD_TYPES.set(skipTypes);
                    try {
                        return super.save(instance);
                    } finally {
                        PrefabDataAccessStrategy.SKIP_CHILD_TYPES.remove();
                    }
                }
            }
        }
        return super.save(instance);
    }

    private <T> void drainEventsToOutbox(T result, RelationalPersistentEntity<T> entity) {
        List<Object> events = PendingEventBuffer.drainAll();
        if (events.isEmpty()) {
            return;
        }
        Outbox outbox = result.getClass().getAnnotation(Outbox.class);
        boolean outboxDisabled = outbox != null && !outbox.enabled();
        if (outboxRepository == null || outboxDisabled) {
            publishDirectly(events);
        } else {
            saveToOutbox(result, entity, events);
        }
    }

    private void publishDirectly(List<Object> events) {
        ApplicationEventPublisher publisher = SpringDomainEventPublisher.getApplicationEventPublisher();
        events.forEach(publisher::publishEvent);
    }

    private <T> void saveToOutbox(T result, RelationalPersistentEntity<T> entity, List<Object> events) {
        String aggregateType = result.getClass().getSimpleName();
        String aggregateId = entity.getIdentifierAccessor(result).getRequiredIdentifier().toString();
        for (Object event : events) {
            outboxRepository.save(new OutboxEntry(
                    UUID.randomUUID().toString(),
                    aggregateType,
                    aggregateId,
                    event.getClass().getName(),
                    serializeToJson(event),
                    Instant.now(),
                    null
            ));
        }
    }

    private String serializeToJson(Object event) {
        if (jsonMapper == null) {
            throw new IllegalStateException("JsonMapper is required for outbox serialization but was not configured");
        }
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event to JSON: " + event.getClass().getName(), e);
        }
    }

    private <T> boolean hasCollectionProperties(RelationalPersistentEntity<T> entity) {
        for (RelationalPersistentProperty property : entity) {
            if (property.isCollectionLike() && !property.isMap()) {
                return true;
            }
        }
        return false;
    }

    private <T> Set<Class<?>> findUnchangedChildTypes(
            RelationalPersistentEntity<T> entity,
            T current,
            T updated
    ) {
        Set<Class<?>> skipTypes = new HashSet<>();
        PersistentPropertyAccessor<T> currentAccessor = entity.getPropertyAccessor(current);
        PersistentPropertyAccessor<T> updatedAccessor = entity.getPropertyAccessor(updated);

        for (RelationalPersistentProperty property : entity) {
            if (!property.isCollectionLike() || property.isMap()) {
                continue;
            }
            Class<?> componentType = getComponentType(property);
            if (componentType == null || !componentType.isRecord()) {
                continue;
            }
            Object currentValue = currentAccessor.getProperty(property);
            Object updatedValue = updatedAccessor.getProperty(property);
            if (Objects.equals(currentValue, updatedValue)) {
                skipTypes.add(componentType);
            }
        }

        return skipTypes;
    }

    private @Nullable Class<?> getComponentType(RelationalPersistentProperty property) {
        var componentType = property.getTypeInformation().getComponentType();
        return componentType != null ? componentType.getType() : null;
    }

    /**
     * Loads the current persisted state for the given id. For concrete subtypes of a polymorphic sealed-interface
     * aggregate, the query is issued against the sealed interface's entity so that the {@code type} discriminator
     * column is included in the SELECT and the registered reading converter can reconstruct the correct subtype.
     */
    @SuppressWarnings("unchecked")
    private <T> @Nullable T findCurrentState(Object id, Class<T> type) {
        Class<?> sealedInterface = PrefabPersistentEntity.findDirectSealedAggregateInterface(type);
        if (sealedInterface != null) {
            return (T) findById(id, sealedInterface);
        }
        return findById(id, type);
    }
}
