package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import java.math.BigDecimal;

/**
 * Revenue and item count for one category in one event-time window.
 *
 * @param categoryId category identifier
 * @param revenue accumulated revenue
 * @param itemCount number of units sold
 */
@Event(topic = "showcase.streams.hourly-category-revenue")
public record HourlyCategoryRevenue(String categoryId, BigDecimal revenue, long itemCount) implements Keyed<String> {
    public static HourlyCategoryRevenue empty() {
        return new HourlyCategoryRevenue(null, BigDecimal.ZERO, 0);
    }

    public HourlyCategoryRevenue add(CategorySale sale) {
        return new HourlyCategoryRevenue(categoryId == null ? sale.categoryId() : categoryId,
                revenue.add(sale.revenue()), itemCount + 1);
    }

    @Override
    public String key() {
        return categoryId;
    }
}
