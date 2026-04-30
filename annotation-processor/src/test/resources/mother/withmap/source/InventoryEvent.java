package mother.withmap.source;

import be.appify.prefab.core.annotations.Event;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "inventory-events")
public sealed interface InventoryEvent permits InventoryEvent.Updated {

    record Updated(String name, Map<String, Integer> stock) implements InventoryEvent {
    }
}
