package be.appify.prefab.example.showcase.shipping;

import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.shipping.infrastructure.http.ShipmentResponse;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.example.showcase.support.SchemaRegistrySubjects;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.data.domain.Pageable;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Schema-first Avro events: the event record is generated from an {@code .avsc} file. The generated test client is
 * used for the shipment REST calls.
 */
@IntegrationTest
class ShipmentIntegrationTest {

    @Autowired
    ShipmentClient shipments;
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;
    @Autowired
    KafkaProperties kafkaProperties;
    @TestEventConsumer(topic = "showcase.shipment")
    EventConsumer<ShipmentDispatched> dispatched;

    @Test
    void placeOrder_preparesAShipmentThatWaitsForDispatch() throws Exception {
        var orderId = placeOrder();

        var shipment = shipmentOf(orderId);

        assertThat(shipment.carrier()).isNull();
        assertThat(shipment.trackingNumber()).isNull();
    }

    @Test
    void dispatch_publishesTheEventGeneratedFromTheSchemaFile() throws Exception {
        var orderId = placeOrder();
        var shipmentId = shipmentOf(orderId).id().id();

        shipments.dispatch(shipmentId, "DHL", "JD0001");

        await().atMost(ofSeconds(30)).untilAsserted(() -> assertThat(dispatched.messages()).anySatisfy(event -> {
            assertThat(event.shipmentId()).isEqualTo(shipmentId);
            assertThat(event.orderId()).isEqualTo(orderId);
            assertThat(event.carrier()).isEqualTo("DHL");
            assertThat(event.trackingNumber()).isEqualTo("JD0001");
        }));
    }

    @Test
    void dispatch_registersTheEventSchemaInTheSchemaRegistry() throws Exception {
        var orderId = placeOrder();
        var shipmentId = shipmentOf(orderId).id().id();

        shipments.dispatch(shipmentId, "DHL", "JD0002");

        await().atMost(ofSeconds(30)).untilAsserted(() ->
                assertThat(SchemaRegistrySubjects.list(kafkaProperties))
                        .as("schema registry subjects")
                        .contains("showcase.shipment-value"));
    }

    private String placeOrder() throws Exception {
        return orders.createOrder(shop.registerCustomer(), shop.stockedProduct(10).id(), 1, BigDecimal.TEN);
    }

    private ShipmentResponse shipmentOf(String orderId) {
        return await().atMost(ofSeconds(20)).until(() -> shipments.findShipments(Pageable.unpaged(), orderId)
                .getContent().stream().findFirst().orElse(null), Objects::nonNull);
    }

}
