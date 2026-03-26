package be.appify.prefab.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field as a unified test event consumer for a specific topic.
 * <p>
 * The field should be of type {@link EventConsumer}. This annotation works with Kafka, Pub/Sub, and SNS/SQS,
 * making it easy to pivot between messaging platforms without changing the test code.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @TestEventConsumer(topic = "${topics.user.name}")
 * EventConsumer<UserEvent> userConsumer;
 * }
 * </pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestEventConsumer {
    /**
     * The topic to consume from. Supports both plain text topic names and Spring property placeholders.
     *
     * @return the topic name
     */
    String topic();
}
