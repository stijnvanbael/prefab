package com.example.catalog;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.ViewOf;
import java.math.BigDecimal;

@ViewOf(Product.class)
@GetList
public record CatalogItem(String name, BigDecimal price, boolean inStock) {
    public CatalogItem(Product product) {
        this(product.name(), product.price(), product.stock() > 0);
    }
}
