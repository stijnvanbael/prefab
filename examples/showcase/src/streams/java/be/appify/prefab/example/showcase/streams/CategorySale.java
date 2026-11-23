package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import java.math.BigDecimal;

/**
 * An order line enriched with its catalog category.
 *
 * @param categoryId category identifier used as the aggregation key
 * @param revenue revenue contributed by this item
 */
@Event(topic = "showcase.streams.category-sale")
public record CategorySale(String categoryId, BigDecimal revenue) implements Keyed<String> {
    @Override
    public String key() {
        return categoryId;
    }
}
