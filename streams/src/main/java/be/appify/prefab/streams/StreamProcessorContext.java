package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

public interface StreamProcessorContext<K, V extends Keyed<K>> {
    void forward(StreamRecord<K, V> value);

    void schedule(Duration interval, Consumer<Instant> task);
}
