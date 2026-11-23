package be.appify.prefab.example.showcase.shipping;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFile;
import be.appify.prefab.core.annotations.Event;

/**
 * The events of a shipment, defined by an Avro schema file instead of a Java record.
 *
 * <p>This is the schema-first way to use Avro, for a contract that is owned by someone else or shared with other
 * languages: Prefab generates the {@code ShipmentDispatched} record from {@code avro/shipment-dispatched.avsc}, uses
 * its {@code shipmentId} field as partitioning key, and fails at startup when the generated schema drifts from the
 * file. The Avro namespace equals this package, because the generated record lives next to this interface.</p>
 */
@Event(topic = "showcase.shipment", serialization = Event.Serialization.AVRO)
@Avsc(files = @AvscFile(path = "avro/shipment-dispatched.avsc", keyProperty = "shipmentId"))
public sealed interface ShipmentEvent permits ShipmentDispatched {
}
