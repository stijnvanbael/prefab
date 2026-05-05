package event.serialization.dependency;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "dependency-event")
public record DependencyEvent(String id) {
}

