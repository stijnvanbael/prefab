package be.appify.prefab.core.spring.data;

import be.appify.prefab.core.annotations.rest.Filter;
import org.jspecify.annotations.Nullable;

/**
 * A range comparison on a single property, used to complement Query by Example (which cannot express ranges).
 *
 * @param property
 *         the property name
 * @param operator
 *         one of the range operators of {@link Filter.Operator}
 * @param value
 *         the value to compare with; the condition is ignored when {@code null}
 */
public record RangeCondition(String property, Filter.Operator operator, @Nullable Object value) {

    /** Validates that the operator is a range operator. */
    public RangeCondition {
        if (!isRange(operator)) {
            throw new IllegalArgumentException("Not a range operator: " + operator);
        }
    }

    /**
     * Whether the given operator is a range operator.
     *
     * @param operator
     *         the operator
     * @return true for GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN and LESS_THAN_OR_EQUAL
     */
    public static boolean isRange(Filter.Operator operator) {
        return switch (operator) {
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> true;
            case EQUAL, CONTAINS, MATCHES_REGEX, STARTS_WITH, ENDS_WITH -> false;
        };
    }

    /**
     * Whether this condition has a value to compare with.
     *
     * @return true if the value is not null
     */
    public boolean isActive() {
        return value != null;
    }
}
