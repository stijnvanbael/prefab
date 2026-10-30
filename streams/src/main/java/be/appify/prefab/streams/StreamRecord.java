package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import be.appify.prefab.streams.tracing.MessageTracing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record StreamRecord<K, V extends Keyed<K>>(
        K key,
        V value,
        Instant timestamp,
        Map<String, String> headers
) {
    /**
     * The ID of the message, present on published messages when tracing is enabled.
     *
     * @return the message ID, if present
     */
    public Optional<String> messageId() {
        return Optional.ofNullable(headers).map(h -> h.get(MessageTracing.MESSAGE_ID_HEADER));
    }

    /**
     * The IDs of the messages this record derives from.
     *
     * @return the correlation IDs; empty when the record does not originate from another message
     */
    public List<String> correlationIds() {
        return headers == null ? List.of() : MessageTracing.decode(headers.get(MessageTracing.CORRELATION_IDS_HEADER));
    }
}