package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.example.showcase.catalog.infrastructure.http.CategoryResponse;
import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

/** Categories: created and listed in name order through the generated client. */
@IntegrationTest
class CategoryIntegrationTest {

    @Autowired
    CategoryClient categories;

    @Test
    void createCategory_givenName_canBeRetrievedById() throws Exception {
        var categoryId = categories.createCategory("Coffee");

        assertThat(categories.getCategoryById(categoryId).name()).isEqualTo("Coffee");
    }

    @Test
    void findCategories_listsThemByName() throws Exception {
        categories.createCategory("Tea");
        categories.createCategory("Coffee");
        categories.createCategory("Cocoa");

        assertThat(categories.findCategories(Pageable.unpaged()).content()).extracting(CategoryResponse::name)
                .containsExactly("Cocoa", "Coffee", "Tea");
    }
}
