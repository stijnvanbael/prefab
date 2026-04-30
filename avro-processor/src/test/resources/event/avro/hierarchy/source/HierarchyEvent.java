package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "hierarchy", serialization = Event.Serialization.AVRO)
public sealed interface HierarchyEvent permits HierarchyEvent.Created, HierarchyEvent.Updated {
    public record Created(
            String id,
            String name
    ) implements HierarchyEvent {
    }

    public record Updated(
            String id,
            String name
    ) implements HierarchyEvent {
    }
}