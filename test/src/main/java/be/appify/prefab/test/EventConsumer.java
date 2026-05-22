package be.appify.prefab.test;

import java.util.ArrayList;
import java.util.List;

/**
 * A unified consumer that collects events from any messaging system (Kafka, Pub/Sub, SNS/SQS).
 * <p>
 * This is the central container for the unified event consumer testing API. Use it in combination with
 * {@link TestEventConsumer} to receive events in integration tests, regardless of the underlying
 * messaging system, making it easy to pivot between messaging platforms.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @TestEventConsumer(topic = "${topics.user.name}")
 * EventConsumer<UserEvent> userConsumer;
 *
 * @Test
 * void createUser() {
 *     userClient.createUser("Alice");
 *
 *     EventAssertions.assertThat(userConsumer)
 *         .hasReceivedValueSatisfying(UserEvent.Created.class, event -> {
 *             assertThat(event.name()).isEqualTo("Alice");
 *         });
 * }
 * }
 * </pre>
 *
 * @param <T>      the type of events
 */
public class EventConsumer<T> {
    private List<T> messages;

    public EventConsumer() {
        this(new ArrayList<>());
    }

    public EventConsumer(List<T> messages) {
        this.messages = messages;
    }

    public void addMessage(T message) {
        messages = new ArrayList<>(messages);
        messages.add(message);
    }

    public List<T> messages() {
        return List.copyOf(messages);
    }

    /**
     * Resets the consumer by clearing all collected events.
     */
    public void reset() {
        messages = new ArrayList<>();
    }
}
