package be.appify.prefab.core.sns;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata of one SQS delivery, for consumers that need more than the deserialized event.
 *
 * @param messageId the SQS message ID, stable across redeliveries of the same message
 * @param attributes the string message attributes that SNS published with the notification
 * @param sentTimestamp when SQS received the message ({@code SentTimestamp})
 * @param messageGroupId the FIFO message group, empty on standard queues
 */
public record SqsMessageMetadata(
        String messageId,
        Map<String, String> attributes,
        Instant sentTimestamp,
        Optional<String> messageGroupId
) {
    /**
     * Name of the attribute that marks a message without a payload, such as the deletion of a record. A consumer
     * with metadata receives {@code null} as the event for such a message.
     */
    public static final String TOMBSTONE_ATTRIBUTE = "prefab-tombstone";

    /** Creates the metadata of a delivery, with an immutable copy of its attributes. */
    public SqsMessageMetadata {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /** @return whether the message is a tombstone, which has no payload */
    public boolean tombstone() {
        return Boolean.parseBoolean(attributes.get(TOMBSTONE_ATTRIBUTE));
    }
}
