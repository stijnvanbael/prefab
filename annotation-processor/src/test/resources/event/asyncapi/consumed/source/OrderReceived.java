package event.asyncapi;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Event(topic = "orders-in", serialization = Event.Serialization.JSON)
public record OrderReceived(
        String orderId,
        String customerId
) {
}
