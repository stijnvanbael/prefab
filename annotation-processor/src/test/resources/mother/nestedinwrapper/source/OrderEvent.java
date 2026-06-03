package mother.nestedinwrapper.source;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;

@Event(topic = "orders")
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record OrderEvent(
        String orderId,
        ShippingAddress shippingAddress) {

    /**
     * Single-value wrapper type whose only component is a multi-field record.
     */
    public record ShippingAddress(Address address) {
        public record Address(String street, String city) {
        }
    }
}

