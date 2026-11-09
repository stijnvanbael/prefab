package be.appify.prefab.core.serialization;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.versioning.EventVersions;

/** Shared publication validation for transports carrying concrete event identity as metadata. */
public final class ConcreteEventSupport {
    private ConcreteEventSupport() {}

    /**
     * Checks the platform declared by a concrete binding.
     *
     * @param type event binding
     * @param platform dispatcher platform
     * @return whether this binding selects the platform or derives it from application configuration
     */
    public static boolean usesPlatform(EventType<?> type, Event.Platform platform) {
        var declared = EventVersions.declarationFor(type).platform();
        return declared == platform || declared == Event.Platform.DERIVED;
    }

    /**
     * Validates a concrete publication and returns its original payload.
     *
     * @param topic target topic
     * @param event publication or ordinary payload
     * @param registry locally registered event bindings
     * @return the original payload, unchanged for ordinary events
     */
    public static Object payloadFor(String topic, Object event, EventRegistry registry) {
        if (event instanceof TypedEvent<?> typed) {
            registry.concreteTypeFor(topic, typed.type().id());
            return typed.value();
        }
        if (event != null && event.getClass().getTypeParameters().length > 0
                && event.getClass().getAnnotationsByType(Event.class).length > 0) {
            throw new IllegalArgumentException("Generic event serialization requires an explicit EventType: "
                    + event.getClass().getName());
        }
        return event;
    }
}
