package pubsub.customdlt;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "${topic.user.name}", platform = Event.Platform.PUB_SUB)
public sealed interface UserEvent permits UserEvent.Created, UserEvent.Updated, UserEvent.Deleted {
    @PartitioningKey
    String id();

    public record Created(
            String id,
            String name
    ) implements UserEvent {
    }

    public record Updated(
            String id,
            String name
    ) implements UserEvent {
    }

    public record Deleted(
            String id
    ) implements UserEvent {
    }
}

