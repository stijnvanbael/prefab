package mother.dependency.source;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "dependency-event")
public record DependencyEvent(String id) {
}

