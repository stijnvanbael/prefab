package com.example.shop.inventory;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.Product;
import java.util.List;

/** Query that {@link StockLevel} uses to find the stock levels an order event is delivered to. */
@RepositoryMixin(StockLevel.class)
public interface StockLevelRepositoryMixin {

    /**
     * Finds the stock levels of a product.
     *
     * @param product the product
     * @return the stock levels of the product
     */
    List<StockLevel> findByProduct(Reference<Product> product);
}
