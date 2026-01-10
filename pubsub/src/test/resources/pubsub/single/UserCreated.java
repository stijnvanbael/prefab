package pubsub.single;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "user", platform = Event.Platform.PUB_SUB)
public record UserCreated(
        @PartitioningKey String id,
        String name
) {
}
