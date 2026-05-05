package pubsub.supertype;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topic.user.name}", platform = Event.Platform.PUB_SUB)
public record UserCreated(String id, String name) implements UserEvent {
}

