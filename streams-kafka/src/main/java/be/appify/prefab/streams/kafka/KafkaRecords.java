package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.StreamRecord;
import java.time.Instant;
import java.util.HashMap;
import org.apache.kafka.streams.processor.api.Record;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Converts Kafka Streams records to Prefab stream records. */
final class KafkaRecords {
    private KafkaRecords() {
    }

    static <K, V extends Keyed<K>> StreamRecord<K, V> toStreamRecord(Record<K, V> record) {
        var headers = new HashMap<String, String>();
        for (var header : record.headers()) {
            headers.put(header.key(), new String(header.value(), UTF_8));
        }
        return new StreamRecord<>(record.key(), record.value(), Instant.ofEpochMilli(record.timestamp()), headers);
    }
}
