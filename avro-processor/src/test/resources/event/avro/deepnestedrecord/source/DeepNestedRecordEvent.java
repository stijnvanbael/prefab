package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "deep-nested-record", serialization = Event.Serialization.AVRO)
public record DeepNestedRecordEvent(
        String id,
        Order order) {
    public record Order(
            String orderId,
            Address shippingAddress
    ) {
        public record Address(
                String street,
                String city
        ) {
        }
    }
}

