package be.appify.prefab.test.pubsub;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field as a test subscriber for a specific topic.
 * The field should be of type {@link be.appify.prefab.test.pubsub.Subscriber}.
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @TestSubscriber(topic = "my-topic")
 * Subscriber<String> subscriber;
 * }
 * </pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestSubscriber {
    /**
     * The topic to subscribe to. Supports both plain text topic names and Spring property placeholders.
     *
     * @return the topic name
     */
    String topic();
}
