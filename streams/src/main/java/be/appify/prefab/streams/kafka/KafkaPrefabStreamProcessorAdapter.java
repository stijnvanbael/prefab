package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KafkaPrefabStreamProcessorAdapter<VI, VO> extends ContextualProcessor<String, VI,String,  VO> {
    private final StreamProcessor<VI, VO> processor;

    public KafkaPrefabStreamProcessorAdapter(StreamProcessor<VI, VO> processor) {
        this.processor = processor;
    }

    @Override
    public void init(ProcessorContext<String, VO> context) {
        super.init(context);
        processor.init(new KafkaPrefabProcessorContext<>(context));
    }

    @Override
    public void process(Record<String, VI> input) {
        processor.process(new StreamRecord<>(
                input.key(),
                input.value(),
                Instant.ofEpochMilli(input.timestamp()),
                toMap(input.headers())
        ));
    }

    private Map<String, String> toMap(Headers headers) {
        var headersMap = new HashMap<String, String>();
        for (var header : headers) {
            headersMap.put(header.key(), new String(header.value(), UTF_8));
        }
        return headersMap;
    }
}
