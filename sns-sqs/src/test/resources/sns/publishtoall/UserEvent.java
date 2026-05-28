package sns.publishtoall;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Event(topic = {"${topic.user.primary}", "${topic.user.secondary}"}, publishTo = PublishTo.ALL, platform = Event.Platform.SNS_SQS)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed interface UserEvent permits UserEvent.Created, UserEvent.Updated {

    record Created(String id, String name) implements UserEvent {
    }

    record Updated(String id, String name) implements UserEvent {
    }
}

