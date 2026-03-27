package be.appify.prefab.test.sns;

import java.util.List;

/**
 * A simple subscriber that collects SQS messages.
 *
 * @param <T> the type of messages
 * @param messages the list of collected messages
 * @deprecated Use {@link be.appify.prefab.test.EventConsumer} instead.
 */
@Deprecated
public record SqsSubscriber<T>(List<T> messages) {
    /**
     * Resets the subscriber by clearing all collected messages.
     */
    public void reset() {
        messages.clear();
    }
}
