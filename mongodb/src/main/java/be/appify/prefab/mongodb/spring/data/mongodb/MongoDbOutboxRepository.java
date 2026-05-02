package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.outbox.OutboxEntry;
import be.appify.prefab.core.outbox.OutboxRepository;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * MongoDB-based implementation of {@link OutboxRepository}.
 * <p>
 * Persists outbox entries to the {@value #COLLECTION_NAME} collection, guaranteeing
 * at-least-once delivery semantics alongside the aggregate state change.
 * </p>
 * <p>
 * Uses its own internal {@link MongoTemplate} built directly from the
 * {@link MongoDatabaseFactory} to avoid a circular dependency with
 * {@link PrefabMongoTemplate}.
 * </p>
 */
public class MongoDbOutboxRepository implements OutboxRepository {

    static final String COLLECTION_NAME = "prefab_outbox";

    private final MongoTemplate mongoTemplate;

    /**
     * Constructs a new MongoDbOutboxRepository.
     *
     * @param mongoDatabaseFactory the factory used to obtain database connections
     */
    public MongoDbOutboxRepository(MongoDatabaseFactory mongoDatabaseFactory) {
        this.mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
    }

    @Override
    public void save(OutboxEntry entry) {
        Document doc = new Document();
        doc.put("_id", entry.id());
        doc.put("aggregate_type", entry.aggregateType());
        doc.put("aggregate_id", entry.aggregateId());
        doc.put("event_type", entry.eventType());
        doc.put("payload", entry.payload());
        doc.put("created_at", Date.from(entry.createdAt()));
        doc.put("published_at", entry.publishedAt() != null ? Date.from(entry.publishedAt()) : null);
        mongoTemplate.insert(doc, COLLECTION_NAME);
    }

    @Override
    public List<OutboxEntry> findPending(int batchSize) {
        Query query = new Query(Criteria.where("published_at").isNull())
                .with(Sort.by(Sort.Direction.ASC, "created_at"))
                .limit(batchSize);
        return mongoTemplate.find(query, Document.class, COLLECTION_NAME).stream()
                .map(this::toOutboxEntry)
                .toList();
    }

    @Override
    public void delete(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, COLLECTION_NAME);
    }

    private OutboxEntry toOutboxEntry(Document doc) {
        Date publishedAt = doc.getDate("published_at");
        return new OutboxEntry(
                doc.getString("_id"),
                doc.getString("aggregate_type"),
                doc.getString("aggregate_id"),
                doc.getString("event_type"),
                doc.getString("payload"),
                doc.getDate("created_at").toInstant(),
                publishedAt != null ? publishedAt.toInstant() : null
        );
    }
}
