package kafka.supertype;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topic.user.name}", platform = Event.Platform.KAFKA)
public record UserCreated(String id, String name) implements UserEvent {
}

