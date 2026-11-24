package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

public interface StreamProcessorContext<K, V extends Keyed<K>> {
    void forward(StreamRecord<K, V> value);

    /**
     * Schedules a wall-clock callback after the supplied interval.
     *
     * <p>Callbacks are best effort and may run again after a failed attempt. Backends can differ in their ownership
     * model; the in-process backend runs one callback per registration across live application instances and does
     * not provide Kafka-style key-partition access.
     *
     * @param interval positive time between callback occurrences, at least one millisecond
     * @param task callback receiving the wall-clock time at invocation
     */
    void schedule(Duration interval, Consumer<Instant> task);

    /**
     * The maximum number of correlation IDs kept on a message when tracing is enabled.
     *
     * @return the cap, oldest IDs are evicted first
     */
    default int maxCorrelationIds() {
        return TracingConfig.DEFAULT_MAX_CORRELATION_IDS;
    }
}
