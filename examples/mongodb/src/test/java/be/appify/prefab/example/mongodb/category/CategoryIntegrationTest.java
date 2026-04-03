package be.appify.prefab.example.mongodb.category;

import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CategoryIntegrationTest {

    @Autowired
    CategoryClient categories;

    @Test
    void createAndRetrieveCategory() throws Exception {
        var categoryId = categories.createCategory("Electronics");

        var category = categories.getCategoryById(categoryId);

        assertThat(category.name()).isEqualTo("Electronics");
        assertThat(category.id().id()).isEqualTo(categoryId);
    }

    @Test
    void listCategories() throws Exception {
        categories.createCategory("Books");
        categories.createCategory("Clothing");

        var page = categories.findCategories(null);

        assertThat(page.content())
                .extracting(be.appify.prefab.example.mongodb.category.infrastructure.http.CategoryResponse::name)
                .containsExactlyInAnyOrder("Books", "Clothing");
    }
}
