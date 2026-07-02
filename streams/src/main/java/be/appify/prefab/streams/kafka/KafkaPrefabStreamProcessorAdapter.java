package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamRecord;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KafkaPrefabStreamProcessorAdapter<KI, VI extends Keyed<KI>, KO, VO extends Keyed<KO>>
        extends ContextualProcessor<KI, VI, KO, VO> {
    private final StreamProcessor<KI, VI, KO, VO> processor;

    public KafkaPrefabStreamProcessorAdapter(StreamProcessor<KI, VI, KO,  VO> processor) {
        this.processor = processor;
    }

    @Override
    public void init(ProcessorContext<KO, VO> context) {
        super.init(context);
        processor.initContext(new KafkaPrefabProcessorContext<>(context));
    }

    @Override
    public void process(Record<KI, VI> input) {
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
