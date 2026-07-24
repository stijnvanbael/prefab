package kafka.mixedcontractandconcrete;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topic.user.name}", platform = Event.Platform.KAFKA)
public sealed interface UserEvent permits UserCreated {
}

