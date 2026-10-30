package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.tracing.MessageTracing;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

/**
 * Processor stages that maintain message and correlation IDs on record headers.
 *
 * <p>Records in flight only carry the correlation IDs of the consumed messages they derive from. A message ID is
 * minted when a message is published, so correlation IDs always refer to messages that exist on a topic.
 */
final class KafkaMessageTracing {
    private KafkaMessageTracing() {
    }

    /**
     * Stage after a source: the consumed message becomes the origin of the record. Its own ID is kept, or a random
     * one is assigned when it has none. IDs already present as correlation are replaced, the consumed message is the
     * parent.
     */
    static <K, V> FixedKeyProcessorSupplier<K, V, V> ingress() {
        return () -> new HeaderStage<>(headers -> {
            var existing = lastValue(headers, MessageTracing.MESSAGE_ID_HEADER);
            var id = existing != null && !existing.isBlank() ? existing : newMessageId();
            headers.remove(MessageTracing.MESSAGE_ID_HEADER);
            headers.remove(MessageTracing.CORRELATION_IDS_HEADER);
            headers.add(MessageTracing.CORRELATION_IDS_HEADER, id.getBytes(StandardCharsets.UTF_8));
        });
    }

    /** Stage before a sink: the published message gets a new message ID. */
    static <K, V> FixedKeyProcessorSupplier<K, V, V> publish() {
        return () -> new HeaderStage<>(headers -> {
            headers.remove(MessageTracing.MESSAGE_ID_HEADER);
            headers.add(MessageTracing.MESSAGE_ID_HEADER, newMessageId().getBytes(StandardCharsets.UTF_8));
        });
    }

    /** Stage before a state-hiding operator: moves the correlation IDs from the record header into the value. */
    static <K, V> FixedKeyProcessorSupplier<K, V, TracedValue<V>> wrap() {
        return () -> new ContextualFixedKeyProcessor<>() {
            @Override
            public void process(FixedKeyRecord<K, V> record) {
                var origins = MessageTracing.decode(lastValue(record.headers(), MessageTracing.CORRELATION_IDS_HEADER));
                context().forward(record.withValue(new TracedValue<>(record.value(), origins)));
            }
        };
    }

    /** Stage after a state-hiding operator: moves the correlation IDs from the value back into the header. */
    static <K, V> FixedKeyProcessorSupplier<K, TracedValue<V>, V> unwrap() {
        return () -> new ContextualFixedKeyProcessor<>() {
            @Override
            public void process(FixedKeyRecord<K, TracedValue<V>> record) {
                var headers = new RecordHeaders(record.headers().toArray());
                headers.remove(MessageTracing.CORRELATION_IDS_HEADER);
                var origins = record.value().origins();
                if (!origins.isEmpty()) {
                    headers.add(MessageTracing.CORRELATION_IDS_HEADER,
                            MessageTracing.encode(origins).getBytes(StandardCharsets.UTF_8));
                }
                context().forward(record.withValue(record.value().value()).withHeaders(headers));
            }
        };
    }

    private static String newMessageId() {
        return UUID.randomUUID().toString();
    }

    private static String lastValue(Headers headers, String name) {
        var header = headers.lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static final class HeaderStage<K, V> extends ContextualFixedKeyProcessor<K, V, V> {
        private final Consumer<Headers> update;

        private HeaderStage(Consumer<Headers> update) {
            this.update = update;
        }

        @Override
        public void process(FixedKeyRecord<K, V> record) {
            var headers = new RecordHeaders(record.headers().toArray());
            update.accept(headers);
            context().forward(record.withHeaders(headers));
        }
    }
}
