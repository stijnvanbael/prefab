package be.appify.prefab.streams.tracing;

/**
 * Configuration of message tracing in streams.
 *
 * @param enabled           whether message and correlation IDs are tracked
 * @param maxCorrelationIds the maximum number of correlation IDs kept on a message; the oldest are evicted first
 */
public record TracingConfig(boolean enabled, int maxCorrelationIds) {
    /** The default maximum number of correlation IDs. */
    public static final int DEFAULT_MAX_CORRELATION_IDS = 100;

    /** Validates the configuration. */
    public TracingConfig {
        if (maxCorrelationIds < 1) {
            throw new IllegalArgumentException("maxCorrelationIds must be at least 1");
        }
    }

    /**
     * Tracing disabled.
     *
     * @return the configuration
     */
    public static TracingConfig disabled() {
        return new TracingConfig(false, DEFAULT_MAX_CORRELATION_IDS);
    }

    /**
     * Tracing enabled with the default cap on correlation IDs.
     *
     * @return the configuration
     */
    public static TracingConfig withTracing() {
        return new TracingConfig(true, DEFAULT_MAX_CORRELATION_IDS);
    }
}
