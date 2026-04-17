package mother.nestedinwrapper.source;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "orders")
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

