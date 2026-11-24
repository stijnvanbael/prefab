package be.appify.prefab.streams.mongodb;

import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Shared single-node MongoDB replica set for the tests: a Testcontainers instance, or the replica set named by the
 * {@code PREFAB_TEST_MONGODB_URI} environment variable (a connection string without database) where Docker is
 * unavailable.
 */
final class MongoTestDatabase {
    private static final String IMAGE = "mongo:9";
    private static final String DATABASE = "prefab_streams_test";
    private static final MongoDBContainer CONTAINER = startContainerUnlessExternal();

    private MongoTestDatabase() {
    }

    static MongoStreamStateRepository freshRepository() {
        var template = template();
        template.getDb().drop();
        MongoStreamSchema.initialize(template);
        return new MongoStreamStateRepository(template, new JsonStreamStateCodec());
    }

    static MongoStreamStateRepository newInstance() {
        return new MongoStreamStateRepository(template(), new JsonStreamStateCodec());
    }

    static MongoTemplate template() {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(uri()));
    }

    private static String uri() {
        var external = System.getenv("PREFAB_TEST_MONGODB_URI");
        if (external != null) {
            return external.endsWith("/") ? external + DATABASE : external + "/" + DATABASE;
        }
        return CONTAINER.getReplicaSetUrl(DATABASE);
    }

    private static MongoDBContainer startContainerUnlessExternal() {
        if (System.getenv("PREFAB_TEST_MONGODB_URI") != null) {
            return null;
        }
        var container = new MongoDBContainer(IMAGE).withReplicaSet();
        container.start();
        return container;
    }
}
