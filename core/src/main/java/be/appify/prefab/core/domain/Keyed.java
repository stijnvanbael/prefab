package be.appify.prefab.core.domain;

public interface Keyed<K extends Key<K>> {
    K key();
}
