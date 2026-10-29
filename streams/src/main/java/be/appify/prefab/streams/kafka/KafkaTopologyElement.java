package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.TopologyElement;
import org.apache.kafka.streams.Topology;

/** A {@link TopologyElement} backed by a Kafka Streams {@link Topology}. */
public interface KafkaTopologyElement extends TopologyElement {
    /**
     * Returns the Kafka Streams topology this element is part of, including every element declared so far on the
     * same {@link be.appify.prefab.streams.PrefabStreams}.
     *
     * @return the native topology
     */
    Topology nativeTopology();
}
