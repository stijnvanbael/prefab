package be.appify.prefab.core.sns;

import java.util.Map;

/**
 * The content of an SNS notification delivered to an SQS queue.
 *
 * @param payload the published message, in the topic's serialization format
 * @param subject the notification subject, which carries the event class name when published by Prefab
 * @param attributes the string message attributes of the notification
 */
public record SqsNotification(String payload, String subject, Map<String, String> attributes) {

    /** Creates a notification, with an immutable copy of its attributes. */
    public SqsNotification {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
