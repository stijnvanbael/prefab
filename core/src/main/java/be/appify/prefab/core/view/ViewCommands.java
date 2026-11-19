package be.appify.prefab.core.view;

/**
 * Marks a Spring component as the command adapter for an aggregate's frontend view.
 *
 * <p>Implementations keep request-to-domain mapping in ordinary Java methods. Methods annotated
 * with {@code @Create}, {@code @Update}, or {@code @Delete} become HTTP operations; unannotated
 * methods remain in-process only.</p>
 *
 * @param <A> aggregate handled by these commands
 */
public interface ViewCommands<A> {
}
