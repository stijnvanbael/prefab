package be.appify.prefab.example.mongodb.category;

import be.appify.prefab.example.mongodb.MongoDbContainerConfiguration;
import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(MongoDbContainerConfiguration.class)
class CategoryIntegrationTest {

    @Autowired
    CategoryClient categories;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Category.class);
    }

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
