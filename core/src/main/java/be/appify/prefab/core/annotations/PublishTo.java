package be.appify.prefab.core.annotations;

/**
 * Controls which topics an event is published to when multiple topics are registered.
 *
 * <p>The strategy is declared on the {@link Event} annotation and stored in the platform-specific
 * registry (e.g. {@code EventRegistry}, {@code PubSubUtil}, {@code SqsUtil}) so it can be applied
 * at runtime despite {@code @Event} having {@code RetentionPolicy.CLASS}.
 */
public enum PublishTo {

    /**
     * Publish only to the first (primary) topic.
     * This is the default and preserves backward-compatible behaviour for single-topic events.
     */
    FIRST,

    /** Publish to every topic registered for the event type. */
    ALL
}

