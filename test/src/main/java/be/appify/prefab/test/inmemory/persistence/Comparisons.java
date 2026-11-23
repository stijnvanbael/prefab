package be.appify.prefab.test.inmemory.persistence;

import java.math.BigDecimal;

/** Ordering of property values for range conditions and derived queries. */
final class Comparisons {

    private Comparisons() {
    }

    /**
     * Compares a stored value with a bound. Numbers are compared by value, regardless of their type.
     *
     * @param actual
     *         the stored value
     * @param bound
     *         the value to compare with
     * @return negative, zero or positive as {@code actual} is less than, equal to or greater than {@code bound}
     * @throws IllegalArgumentException
     *         if the values cannot be compared
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int compare(Object actual, Object bound) {
        if (actual instanceof Number number && bound instanceof Number other) {
            return new BigDecimal(number.toString()).compareTo(new BigDecimal(other.toString()));
        }
        if (actual instanceof Comparable comparable && actual.getClass().isInstance(bound)) {
            return comparable.compareTo(bound);
        }
        throw new IllegalArgumentException("Cannot compare %s with %s".formatted(
                actual.getClass().getName(), bound.getClass().getName()));
    }
}
