package be.appify.prefab.test;

import be.appify.prefab.test.persistence.FlywayChecksumMismatchMigrationStrategy;
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
 * This annotation combines {@link SpringBootTest} and {@link AutoConfigureMockMvc}, and activates the "test" profile.
 * Test-specific configurations for PostgreSQL, Kafka, Pub/Sub, SNS/SQS, and MongoDB are auto-discovered via Spring's
 * autoconfiguration mechanism.
 * <p>
 * When Flyway is on the classpath, this annotation also registers a migration strategy that automatically drops the
 * schema and retries if there is a checksum mismatch for the last applied migration.
 * <p>
 * When Spring Data MongoDB is on the classpath (i.e., when {@code prefab-mongodb} is a dependency), a MongoDB
 * Testcontainer is automatically started and all collections are dropped before each test.
 * <p>
 * Similarly, when the test module is on the classpath, PostgreSQL, Kafka, Pub/Sub, and SNS/SQS test containers are
 * automatically provisioned and configured via Spring Boot autoconfiguration.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FlywayChecksumMismatchMigrationStrategy.class)
public @interface IntegrationTest {
}
