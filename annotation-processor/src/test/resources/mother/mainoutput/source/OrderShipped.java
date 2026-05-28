package mother.mainoutput.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "order-shipped")
@Generate(plugin = MotherPlugin.class, target = OutputTarget.MAIN)
public record OrderShipped(String orderId, String recipient) {
}

