package be.appify.prefab.core.serialization;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.versioning.EventVersions;
import java.util.Arrays;

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
     * Checks the platform declared by an event class, with the same rule as for a concrete binding. The
     * {@link Event} declaration is read from the class, or from the nearest supertype that has one. An event without
     * any declaration, such as one registered by hand, is not tied to a platform.
     *
     * @param type event class
     * @param platform dispatcher platform
     * @return whether the class selects the platform, derives it from application configuration, or declares none
     */
    public static boolean usesPlatform(Class<?> type, Event.Platform platform) {
        var declarations = declarationsFor(type);
        return declarations.length == 0 || Arrays.stream(declarations)
                .anyMatch(declared -> declared.platform() == platform || declared.platform() == Event.Platform.DERIVED);
    }

    private static Event[] declarationsFor(Class<?> type) {
        for (var candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            var declarations = candidate.getAnnotationsByType(Event.class);
            if (declarations.length > 0) {
                return declarations;
            }
            for (var parent : candidate.getInterfaces()) {
                var inherited = declarationsFor(parent);
                if (inherited.length > 0) {
                    return inherited;
                }
            }
        }
        return new Event[0];
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
