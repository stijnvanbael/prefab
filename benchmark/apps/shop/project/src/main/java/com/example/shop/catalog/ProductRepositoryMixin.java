package com.example.shop.catalog;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;

/**
 * Adds queries that the generated list endpoint does not offer to the generated {@code ProductRepository}. Spring
 * Data derives the query from the method name, on every persistence technology.
 */
@RepositoryMixin(Product.class)
public interface ProductRepositoryMixin {

    /**
     * Counts the products in a category.
     *
     * @param category the category
     * @return the number of products in the category
     */
    long countByCategory(Reference<Category> category);
}
