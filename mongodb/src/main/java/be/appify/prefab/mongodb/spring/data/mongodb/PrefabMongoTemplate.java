package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Outbox;
import be.appify.prefab.core.outbox.OutboxEntry;
import be.appify.prefab.core.outbox.OutboxRepository;
import be.appify.prefab.core.outbox.PendingEventBuffer;
import be.appify.prefab.core.spring.SpringDomainEventPublisher;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoWriter;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Custom {@link MongoTemplate} that flushes the {@link PendingEventBuffer} after every aggregate save.
 * <p>
 * When the saved object is annotated with {@link Aggregate}, any events buffered during the save are
 * either persisted to the transactional outbox (when a {@link MongoDbOutboxRepository} is available and
 * the aggregate has not disabled the outbox via {@link Outbox#enabled()}) or published directly via the
 * Spring application event publisher. The outbox is <strong>enabled by default</strong>; add
 * {@code @Outbox(enabled = false)} to an aggregate class to publish events directly without the outbox.
 * </p>
 */
public class PrefabMongoTemplate extends MongoTemplate {

    @Autowired(required = false)
    private @Nullable OutboxRepository outboxRepository;

    private final JsonMapper jsonMapper;

    /**
     * Constructs a new PrefabMongoTemplate.
     *
     * @param mongoDatabaseFactory the factory used to obtain database connections
     * @param mongoConverter       the converter used to map entities to/from BSON documents
     * @param jsonMapper           the JSON mapper used to serialise domain events for the outbox
     */
    public PrefabMongoTemplate(
            MongoDatabaseFactory mongoDatabaseFactory,
            MongoConverter mongoConverter,
            JsonMapper jsonMapper
    ) {
        super(mongoDatabaseFactory, mongoConverter);
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected <T> T doSave(String collectionName, T objectToSave, MongoWriter<T> writer) {
        T result = super.doSave(collectionName, objectToSave, writer);
        if (objectToSave.getClass().isAnnotationPresent(Aggregate.class)) {
            drainEventsToOutbox(result);
        }
        return result;
    }

    private <T> void drainEventsToOutbox(T aggregate) {
        List<Object> events = PendingEventBuffer.drainAll();
        if (events.isEmpty()) {
            return;
        }
        Outbox outbox = aggregate.getClass().getAnnotation(Outbox.class);
        boolean outboxEnabled = outbox == null || outbox.enabled();
        if (outboxEnabled && outboxRepository != null) {
            saveToOutbox(aggregate, events);
        } else {
            publishDirectly(events);
        }
    }

    private void publishDirectly(List<Object> events) {
        var publisher = SpringDomainEventPublisher.getApplicationEventPublisher();
        events.forEach(publisher::publishEvent);
    }

    private <T> void saveToOutbox(T aggregate, List<Object> events) {
        String aggregateType = aggregate.getClass().getSimpleName();
        String aggregateId = extractId(aggregate);
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
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event to JSON: " + event.getClass().getName(), e);
        }
    }

    private static String extractId(Object aggregate) {
        return Arrays.stream(aggregate.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .map(f -> readFieldValue(f, aggregate))
                .orElse(aggregate.toString());
    }

    private static String readFieldValue(Field field, Object target) {
        field.setAccessible(true);
        try {
            Object value = field.get(target);
            return value != null ? value.toString() : "";
        } catch (IllegalAccessException e) {
            return "";
        }
    }
}
