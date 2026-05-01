package be.appify.prefab.core.outbox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thread-local buffer for domain events pending outbox persistence.
 * Events are added during aggregate construction or update, then drained and saved
 * to the outbox table within the same database transaction as the aggregate.
 *
 * <p>For {@code @AsyncCommit} update methods (which do not trigger a repository save),
 * call {@link #publishPending()} after the domain method to drain the buffer and
 * publish events directly via the registered publisher.
 */
public final class PendingEventBuffer {

    private static final ThreadLocal<List<Object>> BUFFER = ThreadLocal.withInitial(ArrayList::new);

    private static volatile Consumer<Object> directPublisher;

    private PendingEventBuffer() {
    }

    /**
     * Registers the direct-publish callback used by {@link #publishPending()}.
     * Called once during application startup by the Spring event-publisher infrastructure.
     *
     * @param publisher a consumer that publishes a single event to the Spring event bus
     */
    public static void setDirectPublisher(Consumer<Object> publisher) {
        directPublisher = publisher;
    }

    /**
     * Adds an event to the current thread's pending buffer.
     *
     * @param event the domain event to buffer
     */
    public static void add(Object event) {
        BUFFER.get().add(event);
    }

    /**
     * Drains all buffered events and publishes them directly via the registered publisher.
     * Intended for {@code @AsyncCommit} update methods that do not trigger a repository save.
     * If no publisher has been registered the buffer is simply cleared.
     */
    public static void publishPending() {
        List<Object> events = drainAll();
        if (directPublisher != null) {
            events.forEach(directPublisher);
        }
    }

    /**
     * Drains and returns all buffered events, clearing the buffer.
     *
     * @return a snapshot of the buffered events; the buffer is empty after this call
     */
    public static List<Object> drainAll() {
        List<Object> events = new ArrayList<>(BUFFER.get());
        BUFFER.remove();
        return events;
    }

    /**
     * Clears the buffer and removes the ThreadLocal to prevent memory leaks in thread-pooled environments.
     */
    public static void clear() {
        BUFFER.remove();
    }
}
