package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "nested-record", serialization = Event.Serialization.AVRO)
public record NestedRecordEvent(
        String id,
        Money totalAmount,
        Money paidAmount
) {
    public record Money(
            String currency,
            double amount
    ) {
    }
}
