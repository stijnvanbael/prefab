package be.appify.prefab.streams.engine;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A broker-neutral stream message, including the metadata needed to process and trace it.
 *
 * @param messageId broker or Prefab message identity
 * @param key record key
 * @param value record value
 * @param headers message attributes
 * @param eventTime event timestamp
 */
public record StreamMessage(String messageId, Object key, Object value, Map<String, String> headers, Instant eventTime) {
    public StreamMessage {
        Objects.requireNonNull(messageId, "messageId must not be null");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        Objects.requireNonNull(eventTime, "eventTime must not be null");
    }

    /** Returns a copy carrying a changed key or value. */
    public StreamMessage withRecord(Object newKey, Object newValue, Map<String, String> newHeaders) {
        return new StreamMessage(messageId, newKey, newValue, newHeaders, eventTime);
    }
}
