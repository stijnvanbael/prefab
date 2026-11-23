package agentcontext.source;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "orders")
public sealed interface OrderEvent permits OrderEvent.Placed {

    record Placed(String orderId) implements OrderEvent {
    }
}
