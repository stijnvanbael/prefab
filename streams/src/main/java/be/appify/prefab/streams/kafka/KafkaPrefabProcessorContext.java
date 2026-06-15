package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KafkaPrefabProcessorContext<K, V> implements StreamProcessorContext<K, V> {
    private final ProcessorContext<K, V> context;

    public KafkaPrefabProcessorContext(ProcessorContext<K, V> context) {
        this.context = context;
    }

    @Override
    public void forward(StreamRecord<K, V> streamRecord) {
        context.forward(new Record<>(
                streamRecord.key(),
                streamRecord.value(),
                streamRecord.timestamp().toEpochMilli(),
                toHeaders(streamRecord.headers())
        ));
    }

    public ProcessorContext<K, V> kafkaContext() {
        return context;
    }

    private Headers toHeaders(Map<String, String> headersMap) {
        var headers = new RecordHeaders();
        headersMap.forEach((key, value) -> headers.add(key, value.getBytes(UTF_8)));
        return headers;
    }
}
