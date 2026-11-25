package com.example.shop.inventory;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.Product;
import com.example.shop.ordering.OrderEvent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * How many items of a product can still be sold. Placing an order reserves stock, cancelling it releases the stock.
 *
 * <p>{@link EventHandlerConfig} tunes the consumers that Prefab generates for the handlers: two threads and at most
 * three delivery attempts before the message goes to the dead letter topic.</p>
 *
 * @param id        unique reference of the stock level
 * @param version   optimistic locking version
 * @param product   the product this stock level is about
 * @param available items that can still be ordered
 * @param reserved  items that are reserved for placed orders
 */
@Aggregate
@GetById
@GetList
@EventHandlerConfig(concurrency = "2", retryLimit = "3")
public record StockLevel(
        @Id Reference<StockLevel> id,
        @Version long version,
        @NotNull @Filter(operator = EQUAL) Reference<Product> product,
        int available,
        int reserved
) {
    /**
     * Starts tracking the stock of a product.
     *
     * @param product   the product
     * @param available the number of items in stock
     */
    @Create
    public StockLevel(@NotNull Reference<Product> product, @PositiveOrZero int available) {
        this(Reference.create(), 0L, product, available, 0);
    }

    /**
     * Reserves the stock for a placed order.
     *
     * @param event the placed order
     * @return the stock level with the items reserved
     */
    @EventHandler
    @Multicast(queryMethod = "findByProduct", parameters = "product")
    public StockLevel onOrderPlaced(OrderEvent.Placed event) {
        return new StockLevel(id, version, product, available - event.quantity(), reserved + event.quantity());
    }

    /**
     * Releases the reserved stock of a cancelled order.
     *
     * @param event the cancelled order
     * @return the stock level with the items available again
     */
    @EventHandler
    @Multicast(queryMethod = "findByProduct", parameters = "product")
    public StockLevel onOrderCancelled(OrderEvent.Cancelled event) {
        return new StockLevel(id, version, product, available + event.quantity(), reserved - event.quantity());
    }
}
