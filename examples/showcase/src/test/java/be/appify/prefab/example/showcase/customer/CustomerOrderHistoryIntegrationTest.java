package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.capability.RequiresCapability;
import be.appify.prefab.example.showcase.customer.application.CustomerOrderHistoryRepository;
import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.ordering.OrderStatus;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static be.appify.prefab.example.showcase.capability.Capability.ORDERED_DELIVERY;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A projection (read model) that is created from {@link CustomerRegistered} and kept up to date by order events. */
@IntegrationTest
class CustomerOrderHistoryIntegrationTest {

    @Autowired
    CustomerOrderHistoryRepository histories;
    @Autowired
    OrderClient orders;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    Shop shop;

    String customer;
    String product;

    @BeforeEach
    void registerCustomer() throws Exception {
        customer = shop.registerCustomer();
        product = shop.stockedProduct(100).id();
    }

    @Test
    void registerCustomer_startsAnEmptyHistory() {
        assertThat(historyOf(customer).orders()).isEmpty();
    }

    @Test
    void placeOrder_addsItToTheHistoryOfTheCustomerOnly() throws Exception {
        var other = shop.registerCustomer();

        var orderId = orders.createOrder(customer, product, 1, BigDecimal.TEN);

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(historyOf(customer).orders())
                        .extracting(entry -> entry.order().id(), CustomerOrderHistory.Entry::status)
                        .containsExactly(tuple(orderId, OrderStatus.PLACED)));
        assertThat(historyOf(other).orders()).isEmpty();
    }

    @Test
    void customerCanReadOnlyTheirOwnOrderHistory() throws Exception {
        var other = shop.registerCustomer();
        var orderId = orders.createOrder(customer, product, 1, BigDecimal.TEN);

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(historyOf(customer).orders())
                        .extracting(entry -> entry.order().id())
                        .containsExactly(orderId));

        mockMvc.perform(get("/customers/{customerId}/order-history", customer).with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(orderId)));
        mockMvc.perform(get("/customers/{customerId}/order-history", customer).with(user(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/customers/{customerId}/order-history", other).with(user(other)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/orders").with(user(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/customer-order-histories").with(user(customer)))
                .andExpect(status().isNotFound());
    }

    /**
     * The placed and cancelled event of one order are consumed by the same consumer, in the order they were
     * published, because they share a partitioning key. If the cancellation overtook the placement, the history
     * would never learn about it.
     */
    @Test
    @RequiresCapability(ORDERED_DELIVERY)
    void cancelOrderRightAfterPlacing_endsUpCancelledInTheHistory() throws Exception {
        for (var i = 0; i < 5; i++) {
            var orderId = orders.createOrder(customer, product, 1, BigDecimal.TEN);
            orders.cancel(orderId);
        }

        await().atMost(ofSeconds(30)).untilAsserted(() ->
                assertThat(historyOf(customer).orders())
                        .hasSize(5)
                        .allSatisfy(entry -> assertThat(entry.status()).isEqualTo(OrderStatus.CANCELLED)));
    }

    private CustomerOrderHistory historyOf(String customerId) {
        return await().atMost(ofSeconds(20)).until(
                () -> histories.findByCustomer(Reference.<Customer>fromId(customerId)).stream().findFirst(),
                Optional::isPresent).orElseThrow();
    }
}
