package com.example.retail;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "sales", serialization = Event.Serialization.AVRO)
public record SalePaid(Reference<Sale> sale, @PartitioningKey Reference<CashRegister> cashRegister, double amount) {
}
