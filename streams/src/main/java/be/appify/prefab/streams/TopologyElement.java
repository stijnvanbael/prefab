package be.appify.prefab.streams;

import org.apache.kafka.streams.Topology;

/**
 * A part of a Prefab topology, such as a {@link StreamDefinition} or a {@link GlobalStore}.
 *
 * <p>All elements declared on the same {@link PrefabStreams} instance resolve to the same native topology, so any
 * of them can be used to run or inspect it.
 */
public interface TopologyElement {
    /**
     * Returns the native topology this element is part of, including every element declared so far on the same
     * {@link PrefabStreams}.
     *
     * @return the native topology
     */
    Topology nativeTopology();
}