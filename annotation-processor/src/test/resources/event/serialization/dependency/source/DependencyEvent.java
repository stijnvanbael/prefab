package event.serialization.dependency;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "dependency-event")
@Generate(plugin = MotherPlugin.class, enabled = false)
public record DependencyEvent(String id) {
}

