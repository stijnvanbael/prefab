The logistics team owns the Avro schema `src/main/resources/avro/shipment-dispatched.avsc`; it is the contract for
the event published when a shipment leaves the warehouse. Do not change the schema and do not write the event class
by hand: generate the Java record from the schema, in package `com.example.shipping` (so it is
`com.example.shipping.ShipmentDispatched`).

Shipments (`com.example.shipping.Shipment`) are dispatched with `POST /shipments/{id}/dispatch` and body
`{"carrier": ..., "trackingNumber": ...}`. On dispatch, publish a `ShipmentDispatched` event on the Kafka topic
`shipments`, serialized as Avro via the schema registry, with `shipmentId` set to the shipment's id and the carrier and
tracking number from the request. Partition by `shipmentId`.
