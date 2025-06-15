package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "${topics.sale}")
public sealed interface SaleEvent permits SaleCompleted {
}
