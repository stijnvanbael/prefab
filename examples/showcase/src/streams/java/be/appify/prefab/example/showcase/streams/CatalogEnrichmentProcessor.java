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
    /** Category of sales whose product is not (yet) in the replicated catalog. */
    static final String UNCATEGORIZED = "uncategorized";

    private final GlobalStore<String, CatalogProduct> catalog;
    private StreamProcessorContext<String, CategorySale> context;

    CatalogEnrichmentProcessor(GlobalStore<String, CatalogProduct> catalog) {
        this.catalog = catalog;
    }

    @Override
    public void process(StreamRecord<Reference<Order>, OrderLine> record) {
        // The replicated catalog is eventually consistent: an order can arrive before its product. Failing here
        // would make the message a poison message that is redelivered forever on brokers without dead-lettering.
        var categoryId = catalog.get(record.value().productId())
                .map(CatalogProduct::categoryId)
                .orElse(UNCATEGORIZED);
        var sale = new CategorySale(categoryId, record.value().revenue());
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
