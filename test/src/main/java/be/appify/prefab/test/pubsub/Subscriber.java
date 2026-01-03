package be.appify.prefab.test.pubsub;

import java.util.List;

/**
 * A simple subscriber that collects messages.
 *
 * @param <T> the type of messages
 * @param messages the list of collected messages
 */
public record Subscriber<T>(List<T> messages) {
    /**
     * Resets the subscriber by clearing all collected messages.
     */
    public void reset() {
        messages.clear();
    }
}
