package kafka.single;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "prefab.user", platform = Event.Platform.KAFKA)
public record UserCreated(
        @PartitioningKey Reference<User> user,
        String name
) {
}
