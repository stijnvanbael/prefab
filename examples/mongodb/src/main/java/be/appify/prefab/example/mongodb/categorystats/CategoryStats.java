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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * Read-model projection that tracks how many products belong to each category.
 * Handlers are order-independent: both {@link CategoryCreated} and {@link ProductCreated} events
 * may arrive in any order across their respective Kafka topics and the final state will be correct.
 */
@Aggregate
@GetList
public record CategoryStats(
        @Id Reference<CategoryStats> id,
        @Version long version,
        Reference<Category> category,
        @Filter String name,
        int totalProducts
) {
    private static final Logger log = LoggerFactory.getLogger(CategoryStats.class);
    private static final String PENDING_NAME = "";

    /**
     * Updates the category name on an existing stats document.
     * Triggered when {@link CategoryCreated} arrives after {@link ProductCreated} has already
     * created a partial document. When no document exists yet, the static companion
     * {@link #onCategoryCreated} is called to create a fresh one.
     */
    @EventHandler
    @Multicast(queryMethod = "findByCategory", parameters = "reference")
    public CategoryStats updateCategoryName(CategoryCreated event) {
        return new CategoryStats(id, version, category, event.name(), totalProducts);
    }

    /**
     * Creates a new stats document when a category is first created and no stats exist yet.
     * Acts as the static companion for {@link #updateCategoryName}: called by the multicast
     * handler when {@link #findByCategory} returns an empty result.
     */
    @EventHandler
    public static CategoryStats onCategoryCreated(CategoryCreated event) {
        return new CategoryStats(Reference.create(), 0L, event.reference(), event.name(), 0);
    }

    /**
     * Increments the product counter whenever a product is added to this category.
     * When no stats document exists yet (i.e. {@link CategoryCreated} has not been processed),
     * the static companion {@link #onProductAdded} creates a partial document that will be
     * updated with the category name once {@link CategoryCreated} arrives.
     */
    @EventHandler
    @Multicast(queryMethod = "findByCategory", parameters = "category")
    public CategoryStats onProductCreated(ProductCreated event) {
        log.info("Handling ProductCreated event for CategoryStats: {}", event);
        return new CategoryStats(id, version, category, name, totalProducts + 1);
    }

    /**
     * Creates a partial stats document when {@link ProductCreated} arrives before
     * {@link CategoryCreated}. The {@link #name} field is left as a placeholder and will be filled
     * in by {@link #updateCategoryName} once the category event is processed.
     */
    @EventHandler
    public static CategoryStats onProductAdded(ProductCreated event) {
        return new CategoryStats(Reference.create(), 0L, event.category(), PENDING_NAME, 1);
    }
}
