package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.function.Consumer;

public abstract class ContextualStreamProcessor<KI, VI extends Keyed<KI>, KO, VO extends Keyed<KO>>
        implements StreamProcessor<KI, VI, KO, VO> {
    private final ThreadLocal<StreamProcessorContext<KO, VO>> context = new ThreadLocal<>();

    /**
     * Forwards a key-value pair to the next processor in the topology. This method creates a new StreamRecord with the current timestamp and an empty headers map, and then calls the forward(StreamRecord) method.
     *
     * @param key   The key to be forwarded.
     * @param value The value to be forwarded.
     */
    protected void forward(KO key, VO value) {
        forward(new StreamRecord<>(key, value, Instant.now(), Collections.emptyMap()));
    }

    /**
     * Forwards a StreamRecord to the next processor in the topology. Implementations should define how the record is forwarded to the downstream processors.
     *
     * @param streamRecord The StreamRecord to be forwarded.
     */
    protected void forward(StreamRecord<KO, VO> streamRecord) {
        var processorContext = context();
        processorContext.forward(streamRecord);
    }

    @Override
    public void initContext(StreamProcessorContext<KO, VO> context) {
        this.context.set(context);
    }

    /**
     * Schedules a task to be executed at a fixed interval. Implementations should define how the task is scheduled and executed based on the provided interval.
     *
     * @param interval The duration between consecutive executions of the task.
     * @param task The task to be executed, which accepts the current timestamp as an argument.
     */
    protected void schedule(Duration interval, Consumer<Instant> task) {
        context().schedule(interval, task);
    }

    private StreamProcessorContext<KO, VO> context() {
        var processorContext = context.get();
        if (processorContext == null) {
            throw new IllegalStateException("Processor context is not initialized, please call init() first");
        }
        return processorContext;
    }
}
