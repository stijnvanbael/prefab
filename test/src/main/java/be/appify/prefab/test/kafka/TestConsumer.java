package be.appify.prefab.test.kafka;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field of a unit test as a test Kafka consumer for a specific topic.
 * The field should be of type {@link org.apache.kafka.clients.consumer.Consumer}.
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @TestConsumer(topic = "my-topic")
 * Consumer<String, String> consumer;
 * }
 * </pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestConsumer {
    /**
     * The Kafka topic to consume from. Supports both plain text topic names and Spring property placeholders.
     *
     * @return the topic name
     */
    String topic();
}
