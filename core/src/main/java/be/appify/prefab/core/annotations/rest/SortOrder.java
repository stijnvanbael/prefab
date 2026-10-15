package be.appify.prefab.core.annotations.rest;

/**
 * Describes a single sort criterion, used by {@link GetList#defaultSort()}.
 */
public @interface SortOrder {
    /**
     * The name of the field to sort on. Must be a field of the aggregate root.
     *
     * @return The field name.
     */
    String field();

    /**
     * The sort direction. Default is ascending.
     *
     * @return The sort direction.
     */
    Direction direction() default Direction.ASC;

    /**
     * Sort direction.
     */
    enum Direction {
        /** Ascending order. */
        ASC,
        /** Descending order. */
        DESC
    }
}
