package pubsub.speltopic;

import be.appify.prefab.core.annotations.Event;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Event(topic = {"#{'${topic.user.names}'.split(',')}", "prefab.user.audit"}, platform = Event.Platform.PUB_SUB)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed interface UserEvent permits UserEvent.Created {

    record Created(String id, String name) implements UserEvent {
    }
}
