package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.StreamProcessor;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

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
        processor.process(KafkaRecords.toStreamRecord(input));
    }
}
