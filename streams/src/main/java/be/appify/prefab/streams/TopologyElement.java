package be.appify.prefab.streams;

/**
 * A part of a Prefab topology, such as a {@link StreamDefinition} or a {@link GlobalStore}.
 *
 * <p>All elements declared on the same {@link PrefabStreams} instance belong to the same topology. The element itself
 * is backend-neutral; a backend may expose its native representation through a more specific interface.
 */
public interface TopologyElement {
}
