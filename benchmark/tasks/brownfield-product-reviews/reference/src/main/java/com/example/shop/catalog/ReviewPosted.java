package com.example.shop.catalog;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

/**
 * Published when a customer reviews a product.
 *
 * @param product the reviewed product
 * @param review  the review
 * @param rating  the rating, from 1 to 5
 */
@Event(topic = "shop.review-posted")
public record ReviewPosted(@PartitioningKey Reference<Product> product, Reference<Review> review, int rating) {
}
