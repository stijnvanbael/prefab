package kafka.dependencyevents;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "prefab.external.user", platform = Event.Platform.KAFKA)
public record ExternalUserCreated(String userId) {
}

