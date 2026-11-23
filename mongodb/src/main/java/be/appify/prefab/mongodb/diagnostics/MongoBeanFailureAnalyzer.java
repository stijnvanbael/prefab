package be.appify.prefab.mongodb.diagnostics;

import be.appify.prefab.core.diagnostics.FailureReport;
import be.appify.prefab.core.diagnostics.MissingBeanFailureAnalyzer;
import java.util.Map;

/** Explains missing MongoDB beans in an application that uses {@code prefab-mongodb}. */
class MongoBeanFailureAnalyzer extends MissingBeanFailureAnalyzer {

    private static final FailureReport MONGO = new FailureReport(
            "prefab-mongodb needs MongoDB beans, but the application context does not contain them.",
            "Check that Spring Boot's MongoDB auto-configuration is not excluded and that "
                    + "spring-boot-starter-data-mongodb is on the classpath. Set spring.mongodb.uri "
                    + "(mongodb://localhost:27017/<database>), or define a MongoDatabaseFactory bean.");

    MongoBeanFailureAnalyzer() {
        super(Map.of(
                "org.springframework.data.mongodb.MongoDatabaseFactory", MONGO,
                "org.springframework.data.mongodb.core.MongoOperations", MONGO,
                "org.springframework.data.mongodb.core.MongoTemplate", MONGO,
                "com.mongodb.client.MongoClient", MONGO));
    }
}
