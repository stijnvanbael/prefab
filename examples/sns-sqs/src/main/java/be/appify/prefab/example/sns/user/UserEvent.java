package be.appify.prefab.example.sns.user;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "user-events", platform = Event.Platform.SNS_SQS)
public sealed interface UserEvent permits UserEvent.Created {
    record Created(Reference<User> user, String name) implements UserEvent {
    }
}
