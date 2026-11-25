package com.example.shop.customer;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

/**
 * A customer closed their account.
 *
 * @param customer the customer that was deactivated
 */
@Event(topic = "shop.customer-deactivated")
public record CustomerDeactivated(@PartitioningKey Reference<Customer> customer) {
}
