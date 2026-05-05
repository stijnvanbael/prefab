package sns.supertype;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "${topic.user.name}", platform = Event.Platform.SNS_SQS)
public sealed interface UserEvent permits UserCreated, UserUpdated {
    @PartitioningKey
    String id();
}

