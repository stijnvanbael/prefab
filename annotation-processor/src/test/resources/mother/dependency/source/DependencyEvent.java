package mother.dependency.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;

@Event(topic = "dependency-event")
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record DependencyEvent(String id) {
}

