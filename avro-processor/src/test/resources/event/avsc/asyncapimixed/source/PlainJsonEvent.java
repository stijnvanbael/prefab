package event.avsc.asyncapimixed;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "mixed-json")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record PlainJsonEvent(String id) {
}
