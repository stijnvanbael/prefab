package be.appify.prefab.test;

import be.appify.prefab.test.kafka.JsonTestConsumerFactoryAutoConfiguration;
import be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to mark integration tests that require the full application context.
 * <p>
 * This annotation combines {@link SpringBootTest} and {@link AutoConfigureMockMvc},
 * and imports test-specific configurations for JSON Kafka consumers and Pub/Sub testing.
 * It also activates the "test" profile.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ JsonTestConsumerFactoryAutoConfiguration.class, PubSubTestAutoConfiguration.class })
public @interface IntegrationTest {
}
