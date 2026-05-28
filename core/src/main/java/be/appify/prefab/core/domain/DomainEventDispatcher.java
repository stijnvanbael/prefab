package be.appify.prefab.core.domain;

/**
 * Strategy interface for infrastructure adapters that can receive domain events directly,
 * bypassing the Spring {@code ApplicationEventPublisher} bus.
 *
 * <p>Implementations are discovered by {@code SpringDomainEventPublisher} at startup. For each
 * published event the publisher asks every dispatcher whether it {@link #canDispatch can handle}
 * the event type; if at least one matches the event is routed directly to all matching dispatchers
 * and the Spring event bus is skipped. Non-matching events (i.e. those not annotated with
 * {@code @Event} and therefore not registered in any infrastructure registry) continue to flow
 * through {@code ApplicationEventPublisher} unchanged.
 */
public interface DomainEventDispatcher {

    /**
     * Returns {@code true} when this dispatcher is capable of handling events of the given type.
     *
     * @param eventType the runtime class of the event candidate
     * @return {@code true} if this dispatcher should receive events of that type
     */
    boolean canDispatch(Class<?> eventType);

    /**
     * Dispatches the event to the underlying infrastructure (Kafka topic, Pub/Sub topic, SNS topic,
     * etc.).
     *
     * <p>This method is only called when {@link #canDispatch} previously returned {@code true} for
     * the same event type.
     *
     * @param event the domain event to dispatch
     */
    void dispatch(Object event);

    /**
     * Dispatches the event to the specified topic overrides instead of the topics that are
     * registered for the event type.
     *
     * <p>When {@code topicOverrides} is empty this falls back to the standard
     * {@link #dispatch(Object)} behaviour (registry-driven routing).
     *
     * <p>Implementations should override this method to honour the supplied topics rather than
     * querying the registry.
     *
     * @param event          the domain event to dispatch
     * @param topicOverrides zero or more topic names to use in place of the registered topics;
     *                       passing an empty array is equivalent to calling {@link #dispatch(Object)}
     */
    default void dispatch(Object event, String... topicOverrides) {
        dispatch(event);
    }

    /**
     * Convenience method that checks for {@code null} and calls {@link #dispatch} only when the event
     * is non-null and {@link #canDispatch} returns {@code true}.
     *
     * @param event the domain event to publish
     */
    default void publish(Object event) {
        if (event != null && canDispatch(event.getClass())) {
            dispatch(event);
        }
    }
}

