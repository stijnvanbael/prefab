package be.appify.prefab.test.inmemory.streams;

/**
 * Forgets the state of the in-memory streams engine. A separate type, so that the test execution listener does not
 * load engine classes in applications without Prefab Streams.
 */
@FunctionalInterface
interface StreamStateReset {
    /** Forgets all stream state. */
    void reset();
}
