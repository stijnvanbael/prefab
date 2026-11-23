package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import java.math.BigDecimal;

record OrderLine(Reference<Order> order, String productId, BigDecimal revenue) implements Keyed<Reference<Order>> {
    @Override
    public Reference<Order> key() {
        return order;
    }
}
