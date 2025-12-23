package be.appify.prefab.test.pubsub;

import java.util.List;

public record Subscriber<T>(List<T> messages) {
    public void reset() {
        messages.clear();
    }
}
