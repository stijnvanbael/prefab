package kafka.single;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "prefab.user", platform = Event.Platform.KAFKA)
public record UserCreated(
        @PartitioningKey String id,
        String name
) {
}
