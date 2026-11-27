package com.example.shipping;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "shipments", serialization = Event.Serialization.AVRO)
@Avsc("avro/shipment-dispatched.avsc")
public sealed interface ShipmentEvent permits ShipmentDispatched {
}
