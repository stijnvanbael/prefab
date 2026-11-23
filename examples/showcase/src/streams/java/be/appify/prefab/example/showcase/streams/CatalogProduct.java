package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

/**
 * A catalog entry replicated into every sales-analytics stream instance.
 *
 * @param productId product identifier used by orders
 * @param categoryId category used to group sales
 */
@Event(topic = "showcase.catalog-product")
public record CatalogProduct(String productId, String categoryId) implements Keyed<String> {
    @Override
    public String key() {
        return productId;
    }
}
