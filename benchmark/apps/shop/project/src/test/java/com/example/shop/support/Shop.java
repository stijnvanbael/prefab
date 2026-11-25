package com.example.shop.support;

import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.CategoryClient;
import com.example.shop.catalog.ProductClient;
import com.example.shop.customer.Customer;
import com.example.shop.customer.CustomerClient;
import com.example.shop.customer.application.CustomerOrderHistoryRepository;
import com.example.shop.inventory.StockLevelClient;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

/**
 * Sets up the data an order needs, through the generated test clients. Multicast handlers use the default fail-and-retry behavior for
 * empty results unless they explicitly opt out, so a test only places orders for customers and products that have their projections.
 */
@Component
public class Shop {

    private final CustomerClient customers;
    private final CustomerOrderHistoryRepository histories;
    private final CategoryClient categories;
    private final ProductClient products;
    private final StockLevelClient stockLevels;

    Shop(
            CustomerClient customers,
            CustomerOrderHistoryRepository histories,
            CategoryClient categories,
            ProductClient products,
            StockLevelClient stockLevels
    ) {
        this.customers = customers;
        this.histories = histories;
        this.categories = categories;
        this.products = products;
        this.stockLevels = stockLevels;
    }

    /**
     * A product with stock.
     *
     * @param id           the product
     * @param stockLevelId the stock level of the product
     */
    public record StockedProduct(String id, String stockLevelId) {
    }

    /**
     * Registers a customer and waits until the order history of the customer exists. That history is created by an
     * event, and later order events are delivered to it.
     *
     * @return the id of the customer
     * @throws Exception if the customer cannot be registered
     */
    public String registerCustomer() throws Exception {
        var customerId = customers.createCustomer("Ada", "Lovelace", "ada-" + UUID.randomUUID() + "@example.com");
        await().atMost(ofSeconds(20)).until(
                () -> !histories.findByCustomer(Reference.<Customer>fromId(customerId)).isEmpty());
        return customerId;
    }

    /**
     * Adds a product to the catalog and starts tracking its stock.
     *
     * @param available the number of items in stock
     * @return the product and its stock level
     * @throws Exception if the product cannot be created
     */
    public StockedProduct stockedProduct(int available) throws Exception {
        var category = categories.createCategory("Coffee");
        var productId = products.createProduct(
                "Espresso machine " + UUID.randomUUID(), null, "Brewtech", category, new BigDecimal("349.99"));
        return new StockedProduct(productId, stockLevels.createStockLevel(productId, available));
    }
}
