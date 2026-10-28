package be.appify.prefab.streams;

import java.time.Duration;
import java.util.Objects;

/**
 * Time window configuration for windowed aggregations.
 *
 * <p>Records are assigned to windows based on their event time. A tumbling window (where
 * {@code advance} equals {@code size}) partitions time into non-overlapping windows; a hopping window
 * (where {@code advance} is smaller than {@code size}) produces overlapping windows.
 *
 * @param size
 *         length of each window
 * @param advance
 *         interval between the starts of consecutive windows
 * @param grace
 *         how long after the end of a window late-arriving records are still accepted
 */
public record AggregationWindow(Duration size, Duration advance, Duration grace) {

    /**
     * Creates a tumbling window.
     *
     * @param size
     *         length of each window
     * @param grace
     *         grace period for late-arriving records
     * @return validated tumbling window
     */
    public static AggregationWindow tumbling(Duration size, Duration grace) {
        return new AggregationWindow(size, size, grace);
    }

    /**
     * Creates a hopping window.
     *
     * @param size
     *         length of each window
     * @param advance
     *         interval between the starts of consecutive windows
     * @param grace
     *         grace period for late-arriving records
     * @return validated hopping window
     */
    public static AggregationWindow hopping(Duration size, Duration advance, Duration grace) {
        return new AggregationWindow(size, advance, grace);
    }

    public AggregationWindow {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(advance, "advance must not be null");
        Objects.requireNonNull(grace, "grace must not be null");
        if (size.isZero() || size.isNegative()) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (advance.isZero() || advance.isNegative()) {
            throw new IllegalArgumentException("advance must be positive");
        }
        if (advance.compareTo(size) > 0) {
            throw new IllegalArgumentException("advance must not be larger than size");
        }
        if (grace.isNegative()) {
            throw new IllegalArgumentException("grace must not be negative");
        }
    }
}
