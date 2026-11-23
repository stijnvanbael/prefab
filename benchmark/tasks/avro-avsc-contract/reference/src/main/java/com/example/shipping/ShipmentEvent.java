package com.example.shipping;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "shipments", serialization = Event.Serialization.AVRO)
@Avsc("avro/shipment-dispatched.avsc")
public sealed interface ShipmentEvent permits ShipmentDispatched {
    @PartitioningKey
    String shipmentId();
}
