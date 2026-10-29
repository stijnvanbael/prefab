package be.appify.prefab.core.versioning;

/**
 * Migrates a historic snapshot of an event (or component) to the next shape. Implementations are annotated with
 * {@link be.appify.prefab.core.annotations.EventMigration} and are plain objects, so they can be unit-tested without a
 * broker.
 *
 * @param <O> the old shape, a hand-written (or generated) snapshot type
 * @param <N> the new shape, either the next snapshot or the current type
 */
@FunctionalInterface
public interface Migration<O, N> {
    /**
     * Migrates the old shape to the new shape.
     *
     * @param old the old shape
     * @return the new shape
     */
    N migrate(O old);
}
