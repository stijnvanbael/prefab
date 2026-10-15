package be.appify.prefab.core.annotations.rest;

/**
 * Describes a single sort criterion, used by {@link GetList#sorted()}.
 */
public @interface SortOrder {
    /**
     * The name of the property to sort on. Must be a property of the aggregate root.
     *
     * @return The property name.
     */
    String property();

    /**
     * The sort direction. Default is ascending.
     *
     * @return The sort direction.
     */
    Direction direction() default Direction.ASCENDING;

    /**
     * Sort direction.
     */
    enum Direction {
        /** Ascending order. */
        ASCENDING,
        /** Descending order. */
        DESCENDING
    }
}
