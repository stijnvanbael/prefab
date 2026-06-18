package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

public interface StreamProcessorContext<K extends Key<K>, V extends Keyed<K>> {
    void forward(StreamRecord<K, V> value);
}
