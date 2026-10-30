package be.appify.prefab.streams.tracing;

import java.util.Arrays;
import java.util.List;

/**
 * Names and encoding of the record headers that carry message tracing information.
 *
 * <p>A message ID identifies a message that was published to a topic. Correlation IDs are the IDs of the messages
 * a message derives from; the header is absent when a message does not originate from another message.
 */
public final class MessageTracing {
    /** Header holding the ID of a published message. */
    public static final String MESSAGE_ID_HEADER = "prefab-message-id";

    /** Header holding the comma-separated IDs of the messages that contributed to a message. */
    public static final String CORRELATION_IDS_HEADER = "prefab-correlation-ids";

    private static final String SEPARATOR = ",";

    private MessageTracing() {
    }

    /**
     * Encodes correlation IDs as a header value.
     *
     * @param correlationIds the IDs to encode
     * @return the header value
     */
    public static String encode(List<String> correlationIds) {
        return String.join(SEPARATOR, correlationIds);
    }

    /**
     * Decodes a correlation IDs header value.
     *
     * @param headerValue the header value, may be null
     * @return the IDs; empty when the header is absent or blank
     */
    public static List<String> decode(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(headerValue.split(SEPARATOR)).map(String::strip).filter(id -> !id.isEmpty()).toList();
    }
}
