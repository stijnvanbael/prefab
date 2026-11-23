package be.appify.prefab.example.showcase.shipping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.example.showcase.ordering.OrderEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * The shipment of an order. It is created when the order is placed and publishes {@link ShipmentDispatched}, an event
 * that Prefab generated from an Avro schema file, when it leaves the warehouse.
 *
 * @param id             unique reference of the shipment
 * @param version        optimistic locking version
 * @param order          the order that is shipped
 * @param carrier        the carrier that delivers it, once dispatched
 * @param trackingNumber the tracking number of the carrier, once dispatched
 */
@Aggregate
@GetById
@GetList
public record Shipment(
        @Id Reference<Shipment> id,
        @Version long version,
        @Filter(operator = EQUAL) Reference<Order> order,
        @Nullable @Size(max = 100) String carrier,
        @Nullable @Size(max = 100) String trackingNumber
) {
    private static final EventPublisher<ShipmentDispatched> DISPATCHED = EventPublisher.bind(ShipmentDispatched.class);

    /**
     * Prepares the shipment of a placed order.
     *
     * @param event the placed order
     * @return a new shipment that has not been dispatched yet
     */
    @EventHandler
    public static Shipment onOrderPlaced(OrderEvent.Placed event) {
        return new Shipment(Reference.create(), 0L, event.order(), null, null);
    }

    /**
     * Hands the shipment to a carrier and publishes {@link ShipmentDispatched}.
     *
     * @param carrier        the carrier that delivers the order
     * @param trackingNumber the tracking number of the carrier
     * @return the dispatched shipment
     */
    @Update(path = "/dispatch", method = "POST")
    public Shipment dispatch(@NotBlank @Size(max = 100) String carrier, @NotBlank @Size(max = 100) String trackingNumber) {
        DISPATCHED.publish(new ShipmentDispatched(id.id(), order.id(), carrier, trackingNumber));
        return new Shipment(id, version, order, carrier, trackingNumber);
    }
}
