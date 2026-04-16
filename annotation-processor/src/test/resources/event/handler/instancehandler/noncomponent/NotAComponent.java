package event.handler.instancehandler.noncomponent;

import be.appify.prefab.core.annotations.EventHandler;

public class NotAComponent {

    @EventHandler(Order.class)
    public void onOrderCreated(OrderCreated event) {
    }
}
