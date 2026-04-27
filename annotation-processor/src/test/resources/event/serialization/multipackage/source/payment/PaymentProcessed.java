package event.serialization.multipackage.payment;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "payment-processed", serialization = Event.Serialization.JSON)
public record PaymentProcessed(String paymentId) {
}

