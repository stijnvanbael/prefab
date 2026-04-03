package be.appify.prefab.test;

import be.appify.prefab.test.kafka.KafkaTestAutoConfiguration;
import be.appify.prefab.test.mongodb.MongoDbTestAutoConfiguration;
import be.appify.prefab.test.persistence.FlywayChecksumMismatchMigrationStrategy;
import be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration;
import be.appify.prefab.test.sns.SnsTestAutoConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to mark integration tests that require the full application context.
 * <p>
 * This annotation combines {@link SpringBootTest} and {@link AutoConfigureMockMvc}, and imports test-specific
 * configurations for JSON Kafka consumers, Pub/Sub and SNS/SQS testing. It also activates the "test" profile.
 * <p>
 * When Flyway is on the classpath, this annotation also registers a migration strategy that automatically drops the
 * schema and retries if there is a checksum mismatch for the last applied migration.
 * <p>
 * When Spring Data MongoDB is on the classpath (i.e., when {@code prefab-mongodb} is a dependency), this annotation
 * also automatically starts a MongoDB Testcontainer and drops all collections before each test.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ KafkaTestAutoConfiguration.class, PubSubTestAutoConfiguration.class, SnsTestAutoConfiguration.class, FlywayChecksumMismatchMigrationStrategy.class, MongoDbTestAutoConfiguration.class })
public @interface IntegrationTest {
}
