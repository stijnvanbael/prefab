package be.appify.prefab.example.mongodb.categorystats;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.mongodb.category.Category;
import be.appify.prefab.example.mongodb.category.CategoryCreated;
import be.appify.prefab.example.mongodb.product.ProductCreated;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * Read-model projection that tracks how many products belong to each category.
 * One {@code CategoryStats} document is created for each category when the {@link CategoryCreated}
 * event arrives, and the {@code totalProducts} counter is incremented for every
 * subsequent {@link ProductCreated} event.
 */
@Aggregate
@GetList
public record CategoryStats(
        @Id Reference<CategoryStats> id,
        @Version long version,
        @NotNull Reference<Category> category,
        @Filter @NotNull String name,
        int totalProducts
) {
    private static final Logger log = LoggerFactory.getLogger(CategoryStats.class);

    /** Creates a new stats document when a category is first created. */
    @EventHandler
    public static CategoryStats onCategoryCreated(CategoryCreated event) {
        return new CategoryStats(Reference.create(), 0L, event.reference(), event.name(), 0);
    }

    /** Increments the product counter whenever a product is added to this category. */
    @EventHandler
    @Multicast(queryMethod = "findByCategory", parameters = "category")
    public CategoryStats onProductCreated(ProductCreated event) {
        log.info("Handling ProductCreated event for CategoryStats: {}", event);
        return new CategoryStats(id, version, category, name, totalProducts + 1);
    }
}
