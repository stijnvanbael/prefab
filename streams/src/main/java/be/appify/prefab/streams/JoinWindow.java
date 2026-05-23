package be.appify.prefab.streams;

import java.time.Duration;
import java.util.Objects;

/**
 * Join window configuration for stream-stream joins.
 *
 * @param timeDifference
 *         maximum allowed event-time difference between matching records
 * @param grace
 *         grace period for late-arriving records
 */
public record JoinWindow(Duration timeDifference, Duration grace) {

    /**
     * Creates a validated join window.
     *
     * @param timeDifference
     *         maximum allowed event-time difference between matching records
     * @param grace
     *         grace period for late-arriving records
     * @return validated join window
     */
    public static JoinWindow of(Duration timeDifference, Duration grace) {
        return new JoinWindow(timeDifference, grace);
    }

    public JoinWindow {
        Objects.requireNonNull(timeDifference, "timeDifference must not be null");
        Objects.requireNonNull(grace, "grace must not be null");
        if (timeDifference.isNegative()) {
            throw new IllegalArgumentException("timeDifference must not be negative");
        }
        if (grace.isNegative()) {
            throw new IllegalArgumentException("grace must not be negative");
        }
    }
}

