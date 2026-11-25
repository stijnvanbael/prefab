package com.example.shop.purchasing;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.Product;
import com.example.shop.inventory.StockRunningLow;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * Asks purchasing to buy more of a product whose stock runs low. Requests are created automatically, never through
 * the API.
 *
 * @param id       unique reference of the request
 * @param version  optimistic locking version
 * @param product  the product to restock
 * @param quantity the number of items to buy
 * @param status   whether the items were delivered
 */
@Aggregate
@GetById(security = @Security(role = "ADMIN"))
@GetList(security = @Security(role = "ADMIN"))
public record RestockRequest(
        @Id Reference<RestockRequest> id,
        @Version long version,
        @Filter(operator = EQUAL) Reference<Product> product,
        int quantity,
        RestockStatus status
) {
    /** How many items purchasing buys per request. */
    public static final int QUANTITY = 20;

    private static final EventPublisher<RestockFulfilled> FULFILLED = EventPublisher.bind(RestockFulfilled.class);

    /**
     * Opens a restock request when the stock of a product runs low.
     *
     * @param event the product whose stock runs low
     * @return the new request
     */
    @EventHandler
    public static RestockRequest onStockRunningLow(StockRunningLow event) {
        return new RestockRequest(Reference.create(), 0L, event.product(), QUANTITY, RestockStatus.OPEN);
    }

    /**
     * Records that the items were delivered, which adds them to the stock. Fulfilling a request twice has no effect.
     *
     * @return the fulfilled request
     */
    @Update(path = "/fulfilment", method = "POST", security = @Security(role = "ADMIN"))
    public RestockRequest fulfil() {
        if (status == RestockStatus.FULFILLED) {
            return this;
        }
        FULFILLED.publish(new RestockFulfilled(product, quantity));
        return new RestockRequest(id, version, product, quantity, RestockStatus.FULFILLED);
    }
}
