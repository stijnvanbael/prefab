package be.appify.prefab.core.view;

import java.util.List;

/**
 * Maps one already-scoped page of aggregates to a page of frontend views.
 *
 * @param <A> aggregate source type
 * @param <V> view type
 */
public interface ViewAssembler<A, V> {
    /**
     * Maps a batch of aggregate rows to views.
     *
     * @param aggregates tenant- and service-scoped source rows
     * @param context request context for enrichment
     * @return one view for each source aggregate
     */
    List<V> assemble(List<A> aggregates, ViewContext context);
}
