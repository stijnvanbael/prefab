package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KafkaPrefabProcessorContext<VO> implements StreamProcessorContext<VO> {
    private final ProcessorContext<String, VO> context;

    public KafkaPrefabProcessorContext(ProcessorContext<String, VO> context) {
        this.context = context;
    }

    @Override
    public void forward(StreamRecord<VO> streamRecord) {
        context.forward(new Record<>(
                streamRecord.key(),
                streamRecord.value(),
                streamRecord.timestamp().toEpochMilli(),
                toHeaders(streamRecord.headers())
        ));
    }

    public ProcessorContext<String, VO> kafkaContext() {
        return context;
    }

    private Headers toHeaders(Map<String, String> headersMap) {
        var headers = new RecordHeaders();
        headersMap.forEach((key, value) -> headers.add(key, value.getBytes(UTF_8)));
        return headers;
    }
}
