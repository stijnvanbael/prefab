package be.appify.prefab.streams.mongodb;

import java.util.List;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

/** Creates the {@code prefab_stream_*} collections and indexes, and checks the deployment supports transactions. */
public final class MongoStreamSchema {
    /** Collection holding the keyed state. */
    public static final String STORE = "prefab_stream_store";
    /** Collection holding the identities of the messages already processed per subscription. */
    public static final String INBOX = "prefab_stream_inbox";
    /** Collection holding the messages waiting to be published. */
    public static final String OUTBOX = "prefab_stream_outbox";
    /** Collection holding the relay lease per topic. */
    public static final String LEASE = "prefab_stream_lease";

    private MongoStreamSchema() {
    }

    /**
     * Fails when the server is a standalone instance, because it cannot run multi-document transactions.
     *
     * @param template the template of the target database
     * @throws MongoTransactionsUnavailableException when transactions are not supported
     */
    public static void requireTransactions(MongoTemplate template) {
        var hello = template.executeCommand(new Document("hello", 1));
        var replicaSet = hello.containsKey("setName");
        var shardedCluster = "isdbgrid".equals(hello.getString("msg"));
        if (!replicaSet && !shardedCluster) {
            throw new MongoTransactionsUnavailableException();
        }
    }

    /**
     * Creates the missing collections and indexes. Existing ones are left untouched.
     *
     * @param template the template of the target database
     */
    public static void initialize(MongoTemplate template) {
        List.of(STORE, INBOX, OUTBOX, LEASE).forEach(collection -> createCollection(template, collection));
        template.indexOps(STORE).createIndex(new Index()
                .named("expiry")
                .on("_id.namespace", Sort.Direction.ASC)
                .on("expiryKey", Sort.Direction.ASC)
                .on("expiresAt", Sort.Direction.ASC)
                .partial(PartialIndexFilter.of(Criteria.where("expiryKey").exists(true))));
        template.indexOps(INBOX).createIndex(new Index().named("recordedAt").on("recordedAt", Sort.Direction.ASC));
        template.indexOps(OUTBOX).createIndex(new Index()
                .named("topicSeq")
                .on("topic", Sort.Direction.ASC)
                .on("seq", Sort.Direction.ASC));
    }

    private static void createCollection(MongoTemplate template, String collection) {
        if (template.collectionExists(collection)) {
            return;
        }
        try {
            template.createCollection(collection);
        } catch (DataAccessException e) {
            if (!template.collectionExists(collection)) {
                throw e;
            }
        }
    }
}
