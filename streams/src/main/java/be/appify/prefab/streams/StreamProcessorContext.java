package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

public interface StreamProcessorContext<K, V extends Keyed<K>> {
    void forward(StreamRecord<K, V> value);

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
