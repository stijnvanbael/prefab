package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.tracing.MessageTracing;
import be.appify.prefab.streams.tracing.TracingConfig;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KafkaPrefabStreamProcessorAdapter<KI, VI extends Keyed<KI>, KO, VO extends Keyed<KO>>
        extends ContextualProcessor<KI, VI, KO, VO> {
    private final StreamProcessor<KI, VI, KO, VO> processor;
    private KafkaPrefabProcessorContext<KO, VO> processorContext;

    private final int maxCorrelationIds;

    public KafkaPrefabStreamProcessorAdapter(StreamProcessor<KI, VI, KO,  VO> processor) {
        this(processor, TracingConfig.DEFAULT_MAX_CORRELATION_IDS);
    }

    public KafkaPrefabStreamProcessorAdapter(StreamProcessor<KI, VI, KO,  VO> processor, int maxCorrelationIds) {
        this.processor = processor;
        this.maxCorrelationIds = maxCorrelationIds;
    }

    @Override
    public void init(ProcessorContext<KO, VO> context) {
        super.init(context);
        this.processorContext = new KafkaPrefabProcessorContext<>(context, maxCorrelationIds);
        processor.initContext(processorContext);
        processor.globalStores().forEach(store -> store.init(processorContext));
    }

    @Override
    public void process(Record<KI, VI> input) {
        var header = input.headers().lastHeader(MessageTracing.CORRELATION_IDS_HEADER);
        processorContext.currentCorrelationIds(header == null ? null : new String(header.value(), UTF_8));
        try {
            processor.process(KafkaRecords.toStreamRecord(input));
        } finally {
            processorContext.currentCorrelationIds(null);
        }
    }
}
