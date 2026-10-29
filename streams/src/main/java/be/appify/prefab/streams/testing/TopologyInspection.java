package be.appify.prefab.streams.testing;

import java.util.List;
import java.util.Objects;

/**
 * Backend-neutral description of the structure of a topology under test.
 *
 * @param description    a stable textual description of all steps, stores and topics
 * @param storeNames     the names of the state stores, including global stores
 * @param globalStoreCount the number of global stores
 * @param subtopologyCount the number of independent sub-topologies
 */
public record TopologyInspection(
        String description,
        List<String> storeNames,
        int globalStoreCount,
        int subtopologyCount
) {
    /** Constructs a new TopologyInspection. */
    public TopologyInspection {
        Objects.requireNonNull(description, "description must not be null");
        storeNames = List.copyOf(storeNames);
    }
}
