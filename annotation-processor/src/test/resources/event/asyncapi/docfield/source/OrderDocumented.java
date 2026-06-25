package event.asyncapi;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "orders", serialization = Event.Serialization.JSON)
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record OrderDocumented(
        @Doc("Unique identifier of the order") String orderId,
        String customerId
) {
}

