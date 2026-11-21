package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.core.spring.Page;
import be.appify.prefab.example.showcase.capability.RequiresCapability;
import be.appify.prefab.example.showcase.catalog.infrastructure.http.ProductResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static be.appify.prefab.example.showcase.capability.Capability.FULL_TEXT_SEARCH;
import static be.appify.prefab.example.showcase.capability.Capability.RANGE_FILTER;
import static org.assertj.core.api.Assertions.assertThat;

/** {@code @GetList} on products: filters, paging and sorting. */
@IntegrationTest
class ProductListIntegrationTest {

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    String coffee;
    String tea;

    @BeforeEach
    void createCatalog() throws Exception {
        coffee = categories.createCategory("Coffee");
        tea = categories.createCategory("Tea");
        products.createProduct("Espresso machine", "Pump driven espresso maker with steam wand", "Brewtech", coffee,
                new BigDecimal("349.99"));
        products.createProduct("Burr grinder", "Grinds coffee beans for espresso and filter", "Brewtech", coffee,
                new BigDecimal("99.00"));
        products.createProduct("French press", "Brews coffee by steeping", "Pressly", coffee,
                new BigDecimal("29.95"));
        products.createProduct("Tea kettle", "Gooseneck kettle with temperature control", "Pressly", tea,
                new BigDecimal("59.00"));
    }

    @Test
    void findProducts_withoutSort_ordersByNameAsDeclaredOnGetList() throws Exception {
        assertThat(names(products.findProducts(Pageable.unpaged(), null, null, null, null, null)))
                .containsExactly("Burr grinder", "Espresso machine", "French press", "Tea kettle");
    }

    @Test
    void findProducts_givenSort_overridesTheDefaultOrder() throws Exception {
        var byPriceDescending = PageRequest.of(0, 10, Sort.by("price").descending());

        assertThat(names(products.findProducts(byPriceDescending, null, null, null, null, null)))
                .containsExactly("Espresso machine", "Burr grinder", "Tea kettle", "French press");
    }

    @Test
    void findProducts_givenPage_returnsThatSliceAndTheTotal() throws Exception {
        var secondPage = products.findProducts(PageRequest.of(1, 3), null, null, null, null, null);

        assertThat(names(secondPage)).containsExactly("Tea kettle");
        assertThat(secondPage.getTotalElements()).isEqualTo(4);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findProducts_givenBrand_returnsOnlyProductsOfThatBrand() throws Exception {
        assertThat(names(products.findProducts(Pageable.unpaged(), null, "Pressly", null, null, null)))
                .containsExactly("French press", "Tea kettle");
    }

    @Test
    void findProducts_givenCategory_returnsOnlyProductsInThatCategory() throws Exception {
        assertThat(names(products.findProducts(Pageable.unpaged(), null, null, tea, null, null)))
                .containsExactly("Tea kettle");
    }

    @Test
    @RequiresCapability(RANGE_FILTER)
    void findProducts_givenPriceRange_includesBothBounds() throws Exception {
        var between = products.findProducts(Pageable.unpaged(), null, null, null,
                new BigDecimal("29.95"), new BigDecimal("99.00"));

        assertThat(names(between)).containsExactly("Burr grinder", "French press", "Tea kettle");
    }

    @Test
    @RequiresCapability(RANGE_FILTER)
    void findProducts_givenRangeAndEqualityFilters_combinesThem() throws Exception {
        var cheapBrewtech = products.findProducts(Pageable.unpaged(), null, "Brewtech", null,
                null, new BigDecimal("100"));

        assertThat(names(cheapBrewtech)).containsExactly("Burr grinder");
    }

    @Test
    @RequiresCapability(FULL_TEXT_SEARCH)
    void findProducts_givenQuery_searchesNameAndDescription() throws Exception {
        var espresso = products.findProducts(Pageable.unpaged(), "espresso", null, null, null, null);

        assertThat(names(espresso)).containsExactlyInAnyOrder("Espresso machine", "Burr grinder");
    }

    @Test
    @RequiresCapability(FULL_TEXT_SEARCH)
    void findProducts_givenQueryAndRelevanceSort_ranksMatchesInTheNameFirst() throws Exception {
        var byRelevance = PageRequest.of(0, 10, Sort.by("relevance"));

        var espresso = products.findProducts(byRelevance, "espresso", null, null, null, null);

        assertThat(names(espresso)).first().isEqualTo("Espresso machine");
    }

    @Test
    @RequiresCapability(FULL_TEXT_SEARCH)
    void findProducts_givenQueryWithExcludedWord_leavesOutMatchesOfThatWord() throws Exception {
        var coffeeWithoutEspresso = products.findProducts(Pageable.unpaged(), "coffee -espresso", null, null, null,
                null);

        assertThat(names(coffeeWithoutEspresso)).containsExactly("French press");
    }

    @Test
    @RequiresCapability(FULL_TEXT_SEARCH)
    void findProducts_givenQueryAndFilter_combinesThem() throws Exception {
        var pressly = products.findProducts(Pageable.unpaged(), "coffee", "Pressly", null, null, null);

        assertThat(names(pressly)).containsExactly("French press");
    }

    private static List<String> names(Page<ProductResponse> page) {
        return page.content().stream().map(ProductResponse::name).toList();
    }
}
