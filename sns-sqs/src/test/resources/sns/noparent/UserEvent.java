package sns.noparent;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public interface UserEvent {

    @Event(topic = "user", platform = Event.Platform.SNS_SQS)
    public record Created(
            @PartitioningKey
            String id,
            String name
    ) {
    }

    @Event(topic = "user", platform = Event.Platform.SNS_SQS)
    public record Updated(
            @PartitioningKey
            String id,
            String name
    ) {
    }

    @Event(topic = "user", platform = Event.Platform.SNS_SQS)
    public record Deleted(
            @PartitioningKey
            String id
    ) {
    }
}
