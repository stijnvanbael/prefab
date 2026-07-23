package event.handler.mergedhandlersupertype;

public sealed interface OrderEvent permits OrderEvent.Created {
    String orderId();

    record Created(String orderId) implements OrderEvent {
    }
}

