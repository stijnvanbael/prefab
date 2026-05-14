package kafka.offsetoverride;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.types.Reference;

@Event(topic = "prefab.user")
public record UserCreated(@PartitioningKey Reference<User> reference, String username) {
}

