package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

/**
 * A catalog entry replicated into every sales-analytics stream instance.
 *
 * @param productId product identifier used by orders
 * @param categoryId category used to group sales
 */
@Event(topic = CatalogProduct.TOPIC)
public record CatalogProduct(String productId, String categoryId) implements Keyed<String> {
    /** Kafka topic used as the replicated catalog source. */
    public static final String TOPIC = "showcase.catalog-product";

    @Override
    public String key() {
        return productId;
    }
}
