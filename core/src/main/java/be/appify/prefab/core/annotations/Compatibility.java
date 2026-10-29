package be.appify.prefab.core.annotations;

/**
 * Compatibility mode of an event contract, mirroring the Confluent Schema Registry semantics. The annotation
 * processor checks every change of the contract against the committed baseline according to this mode.
 */
public enum Compatibility {
    /** Use the global default, configured with the {@code prefab.events.compatibility} processor option. */
    DEFAULT,
    /** New consumers can read data written with the previous contract. */
    BACKWARD,
    /** Old consumers can read data written with the new contract. */
    FORWARD,
    /** Both {@link #BACKWARD} and {@link #FORWARD}. */
    FULL,
    /** Contract changes are not checked for compatibility. */
    NONE
}
