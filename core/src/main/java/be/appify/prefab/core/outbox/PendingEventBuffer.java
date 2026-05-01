package be.appify.prefab.core.outbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-local buffer for domain events pending outbox persistence.
 * Events are added during aggregate construction or update, then drained and saved
 * to the outbox table within the same database transaction as the aggregate.
 */
public final class PendingEventBuffer {

    private static final ThreadLocal<List<Object>> BUFFER = ThreadLocal.withInitial(ArrayList::new);

    private PendingEventBuffer() {
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
     * Drains and returns all buffered events, clearing the buffer.
     *
     * @return a snapshot of the buffered events; the buffer is empty after this call
     */
    public static List<Object> drainAll() {
        List<Object> events = new ArrayList<>(BUFFER.get());
        BUFFER.get().clear();
        return events;
    }

    /**
     * Clears the buffer without returning its contents.
     */
    public static void clear() {
        BUFFER.get().clear();
    }
}
