package mother.events.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Example;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "person-events")
public sealed interface PersonEvent permits PersonEvent.Created, PersonEvent.Updated {

    record Created(
            @Example("00000000-0000-0000-0000-000000000001") String personId,
            @Example("Alice") String name) implements PersonEvent {
    }

    record Updated(
            @Example("00000000-0000-0000-0000-000000000001") String personId,
            @Example("Bob") String name) implements PersonEvent {
    }
}

