package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import java.math.BigDecimal;

/**
 * Running revenue and item count for one catalog category.
 *
 * @param categoryId category identifier
 * @param revenue accumulated revenue
 * @param itemCount number of units sold
 */
@Event(topic = "showcase.streams.category-revenue")
public record CategoryRevenue(String categoryId, BigDecimal revenue, long itemCount) implements Keyed<String> {
    public static CategoryRevenue empty() {
        return new CategoryRevenue(null, BigDecimal.ZERO, 0);
    }

    public CategoryRevenue add(CategorySale sale) {
        return new CategoryRevenue(categoryId == null ? sale.categoryId() : categoryId,
                revenue.add(sale.revenue()), itemCount + 1);
    }

    @Override
    public String key() {
        return categoryId;
    }
}
