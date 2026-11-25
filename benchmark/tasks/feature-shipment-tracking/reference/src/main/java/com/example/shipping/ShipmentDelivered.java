package com.example.shipping;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "deliveries")
public record ShipmentDelivered(@PartitioningKey Reference<Shipment> shipment, String orderNumber, String address) {
}
