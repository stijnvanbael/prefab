package be.appify.prefab.example.mongodb.categorystats;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.mongodb.category.Category;
import java.util.List;

/** Repository mixin that provides query methods for {@link CategoryStats}. */
@RepositoryMixin(CategoryStats.class)
public interface CategoryStatsRepositoryMixin {

    /**
     * Returns all category stats for the given category.
     *
     * @param category the category to search for
     * @return list of matching category stats
     */
    List<CategoryStats> findByCategory(Reference<Category> category);
}
