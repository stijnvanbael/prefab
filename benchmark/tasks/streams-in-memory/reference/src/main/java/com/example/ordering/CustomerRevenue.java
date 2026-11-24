package com.example.ordering;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import java.math.BigDecimal;

@Event(topic = "customer-revenue")
public record CustomerRevenue(String customer, BigDecimal total) implements Keyed<String> {
    static CustomerRevenue empty() {
        return new CustomerRevenue(null, BigDecimal.ZERO);
    }

    CustomerRevenue add(OrderPlaced placed) {
        return new CustomerRevenue(placed.customer(), total.add(placed.total()));
    }

    @Override
    public String key() {
        return customer;
    }
}
