package kafka.consumefromtopics;

import be.appify.prefab.core.annotations.Event;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Event(topic = {"${topic.user.primary}", "${topic.user.secondary}"}, platform = Event.Platform.KAFKA)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed interface UserEvent permits UserEvent.Created, UserEvent.Updated {

    record Created(String id, String name) implements UserEvent {
    }

    record Updated(String id, String name) implements UserEvent {
    }
}

