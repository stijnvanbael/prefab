package pubsub.single;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "user", platform = Event.Platform.PUB_SUB)
public record UserCreated(
        @PartitioningKey Reference<User> user,
        String name
) {
}
