package com.example.shipping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record DeliveryNotification(
        @Id Reference<DeliveryNotification> id,
        @Version long version,
        Reference<Shipment> shipment,
        @Filter String orderNumber,
        String address
) {
    @EventHandler
    public static DeliveryNotification onShipmentDelivered(ShipmentDelivered event) {
        return new DeliveryNotification(Reference.create(), 0L, event.shipment(), event.orderNumber(), event.address());
    }
}
