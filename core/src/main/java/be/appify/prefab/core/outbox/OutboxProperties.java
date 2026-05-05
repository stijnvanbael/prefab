package be.appify.prefab.core.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the transactional outbox relay.
 * All properties are prefixed with {@code prefab.outbox}.
 */
@ConfigurationProperties(prefix = "prefab.outbox")
public class OutboxProperties {

    private long pollIntervalMs = 1000;
    private int batchSize = 100;

    /**
     * Returns the interval in milliseconds between relay poll cycles.
     *
     * @return the poll interval in milliseconds
     */
    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    /**
     * Sets the interval in milliseconds between relay poll cycles.
     *
     * @param pollIntervalMs the poll interval to set
     */
    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    /**
     * Returns the maximum number of outbox entries processed per relay cycle.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the maximum number of outbox entries processed per relay cycle.
     *
     * @param batchSize the batch size to set
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
