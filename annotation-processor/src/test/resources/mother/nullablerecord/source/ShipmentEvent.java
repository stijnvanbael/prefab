package mother.nullablerecord.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import jakarta.annotation.Nullable;
import java.util.List;

@Event(topic = "shipments")
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record ShipmentEvent(
        String shipmentId,
        @Nullable Address address,
        List<Item> items,
        @Nullable List<Item> optionalItems
) {
    public record Address(String street, String city) {}

    public record Item(String name, int quantity) {}
}

