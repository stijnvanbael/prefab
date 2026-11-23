package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;
import java.util.Collection;
import java.util.List;

final class CatalogEnrichmentProcessor
        implements StreamProcessor<Reference<Order>, OrderLine, String, CategorySale> {
    private final GlobalStore<String, CatalogProduct> catalog;
    private StreamProcessorContext<String, CategorySale> context;

    CatalogEnrichmentProcessor(GlobalStore<String, CatalogProduct> catalog) {
        this.catalog = catalog;
    }

    @Override
    public void process(StreamRecord<Reference<Order>, OrderLine> record) {
        var product = catalog.get(record.value().productId()).orElseThrow(() ->
                new IllegalStateException("No catalog entry for product '%s' in order '%s'"
                        .formatted(record.value().productId(), record.value().order().id())));
        var sale = new CategorySale(product.categoryId(), record.value().revenue());
        context.forward(new StreamRecord<>(sale.key(), sale, record.timestamp(), record.headers()));
    }

    @Override
    public Collection<GlobalStore<?, ?>> globalStores() {
        return List.of(catalog);
    }

    @Override
    public void initContext(StreamProcessorContext<String, CategorySale> context) {
        this.context = context;
    }
}
