package be.appify.prefab.core.annotations;

/**
 * Controls whether a generated aggregate operation is exposed over HTTP.
 */
public enum Exposure {
    /** Exposes the operation as an HTTP endpoint. */
    PUBLIC,
    /** Generates the in-process service operation without an HTTP endpoint. */
    INTERNAL,
    /** Inherits the aggregate-level exposure setting. */
    INHERIT
}
