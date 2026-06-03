package mother.withmap.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "inventory-events")
@Generate(plugin = AssertionPlugin.class, enabled = false)
public sealed interface InventoryEvent permits InventoryEvent.Updated {

    record Updated(String name, Map<String, Integer> stock) implements InventoryEvent {
    }
}
