package com.example.crm;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "customers")
public record CustomerRegistered(@PartitioningKey Reference<Customer> customer, String name) {
}
