package be.appify.prefab.example.showcase.support;

import be.appify.prefab.example.showcase.catalog.CategoryClient;
import be.appify.prefab.example.showcase.catalog.ProductClient;
import be.appify.prefab.example.showcase.customer.CustomerClient;
import be.appify.prefab.example.showcase.customer.CustomerOrderHistoryClient;
import be.appify.prefab.example.showcase.inventory.StockLevelClient;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

/**
 * Sets up the data an order needs, through the generated test clients. Event handlers that find no aggregate for an
 * event fail and retry, so a test only places orders for customers and products that have their projections.
 */
@Component
public class Shop {

    private final CustomerClient customers;
    private final CustomerOrderHistoryClient histories;
    private final CategoryClient categories;
    private final ProductClient products;
    private final StockLevelClient stockLevels;

    Shop(
            CustomerClient customers,
            CustomerOrderHistoryClient histories,
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
                () -> !histories.findCustomerOrderHistories(Pageable.unpaged(), customerId).getContent().isEmpty());
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
