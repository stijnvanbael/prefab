package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code @Autocomplete} suggests distinct values of a field that start with what the user typed. */
@IntegrationTest
class AutocompleteIntegrationTest {

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Test
    void autocompleteByName_givenPrefix_suggestsMatchingCategoriesIgnoringCase() throws Exception {
        categories.createCategory("Coffee");
        categories.createCategory("Cocoa");
        categories.createCategory("Tea");

        assertThat(categories.autocompleteByName("co")).containsExactlyInAnyOrder("Coffee", "Cocoa");
    }

    @Test
    void autocompleteByBrand_suggestsEveryBrandOnce() throws Exception {
        var coffee = categories.createCategory("Coffee");
        products.createProduct("Espresso machine", null, "Brewtech", coffee, BigDecimal.TEN);
        products.createProduct("Burr grinder", null, "Brewtech", coffee, BigDecimal.TEN);
        products.createProduct("Kettle", null, "Brewmaster", coffee, BigDecimal.TEN);
        products.createProduct("French press", null, "Pressly", coffee, BigDecimal.TEN);

        assertThat(products.autocompleteByBrand("BREW")).containsExactlyInAnyOrder("Brewtech", "Brewmaster");
    }

    @Test
    void autocompleteByName_givenLimit_returnsAtMostThatManySuggestions() throws Exception {
        categories.createCategory("Coffee");
        categories.createCategory("Cocoa");
        categories.createCategory("Cookware");

        assertThat(categories.autocompleteByName("co", PageRequest.of(0, 2))).hasSize(2);
    }
}
