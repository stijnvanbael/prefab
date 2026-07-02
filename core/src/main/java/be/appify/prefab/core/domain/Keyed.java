package be.appify.prefab.core.domain;

import be.appify.prefab.core.annotations.PartitioningKey;

public interface Keyed<K> {
    @PartitioningKey
    K key();
}
