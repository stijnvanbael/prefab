package be.appify.prefab.test.mongodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;

/**
 * Test execution listener that removes all documents from every MongoDB collection before each test instance is
 * prepared.
 * <p>
 * This listener activates only when {@code spring-data-mongodb} is on the classpath. It ensures that each test starts
 * without data. The collections themselves are kept, because dropping a collection also drops the indexes created at
 * startup, such as the text index that full-text filters need and unique indexes.
 * </p>
 */
public class MongoDbTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(MongoDbTestExecutionListener.class);
    private static final String MONGO_OPERATIONS_CLASS = "org.springframework.data.mongodb.core.MongoOperations";

    /**
     * Constructs a new MongoDbTestExecutionListener.
     */
    public MongoDbTestExecutionListener() {
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        if (!ClassUtils.isPresent(MONGO_OPERATIONS_CLASS, testContext.getApplicationContext().getClassLoader())) {
            return;
        }
        var mongoOperationsMap = testContext.getApplicationContext().getBeansOfType(MongoOperations.class);
        mongoOperationsMap.values().forEach(ops ->
                ops.getCollectionNames().forEach(collection -> {
                    log.debug("Removing all documents from MongoDB collection: {}", collection);
                    ops.remove(new Query(), collection);
                }));
    }
}
