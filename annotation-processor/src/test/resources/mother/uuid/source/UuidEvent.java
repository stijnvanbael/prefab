package mother.uuid.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import java.util.UUID;

@Event(topic = "uuid-events")
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record UuidEvent(
        UUID id,
        @Example("00000000-0000-0000-0000-000000000001") UUID exampleId) {
}
