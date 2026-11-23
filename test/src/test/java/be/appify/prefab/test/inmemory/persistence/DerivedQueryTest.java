package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.test.inmemory.InMemoryPersistenceAutoConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.FixtureConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Item;
import be.appify.prefab.test.inmemory.persistence.fixture.ItemRepository;
import be.appify.prefab.test.inmemory.persistence.broken.BrokenConfiguration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DerivedQueryTest {
    private static final Instant FEBRUARY = Instant.parse("2024-02-01T00:00:00Z");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryPersistenceAutoConfiguration.class))
            .withPropertyValues("prefab.test.containers=false");

    /** A query on the repository, returning the matching items. */
    interface Query extends Function<ItemRepository, Collection<Item>> {
    }

    static Stream<Arguments> keywords() {
        return Stream.of(
                query("simple property", repository -> repository.findByCategory("coffee"), "i1", "i2"),
                query("negating simple property", repository -> repository.findByCategoryNot("coffee"), "i3", "i4"),
                query("in", repository -> repository.findByCategoryIn(List.of("tea", "coffee")), "i1", "i2", "i3"),
                query("not in", repository -> repository.findByCategoryNotIn(List.of("coffee")), "i3", "i4"),
                query("like", repository -> repository.findByNameLike("Esp%"), "i1"),
                query("like with single character wildcard", repository -> repository.findByNameLike("M_g"), "i4"),
                query("not like", repository -> repository.findByNameNotLike("Esp%"), "i2", "i3", "i4"),
                query("containing", repository -> repository.findByNameContaining("press"), "i1", "i2"),
                query("not containing", repository -> repository.findByNameNotContaining("press"), "i3", "i4"),
                query("starting with", repository -> repository.findByNameStartingWith("Tea"), "i3"),
                query("ending with", repository -> repository.findByNameEndingWith("er"), "i2"),
                query("greater than", repository -> repository.findByStockGreaterThan(5), "i2", "i4"),
                query("greater than equal", repository -> repository.findByStockGreaterThanEqual(5), "i1", "i2", "i4"),
                query("less than", repository -> repository.findByStockLessThan(5), "i3"),
                query("less than equal", repository -> repository.findByStockLessThanEqual(5), "i1", "i3"),
                query("before", repository -> repository.findByCreatedAtBefore(FEBRUARY), "i1"),
                query("after", repository -> repository.findByCreatedAtAfter(FEBRUARY), "i3", "i4"),
                query("between", repository -> repository.findByStockBetween(5, 10), "i1", "i2"),
                query("is null", ItemRepository::findByCategoryIsNull, "i4"),
                query("is not null", ItemRepository::findByCategoryIsNotNull, "i1", "i2", "i3"),
                query("true", ItemRepository::findByActiveTrue, "i1", "i3", "i4"),
                query("false", ItemRepository::findByActiveFalse, "i2"),
                query("regex", repository -> repository.findByNameRegex("^Esp.*e$"), "i1"),
                query("ignore case", repository -> repository.findByNameIgnoreCase("TEAPOT"), "i3"),
                query("containing on a collection", repository -> repository.findByTagsContaining("b"), "i1", "i2"),
                query("and", repository -> repository.findByCategoryAndActive("coffee", true), "i1"),
                query("or", repository -> repository.findByCategoryOrName("tea", "Mug"), "i3", "i4"),
                query("distinct", repository -> repository.findDistinctByCategory("coffee"), "i1", "i2"));
    }

    private static Arguments query(String keyword, Query query, String... expectedIds) {
        return Arguments.of(keyword, query, List.of(expectedIds));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keywords")
    void derivedQuery_supportsTheKeyword(String keyword, Query query, List<String> expectedIds) {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(query.apply(items)).extracting(Item::id).containsExactlyInAnyOrderElementsOf(expectedIds);
        });
    }

    @Test
    void derivedQuery_withOrderBy_sorts() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(items.findByCategoryOrderByNameDesc("coffee")).extracting(Item::id).containsExactly("i2", "i1");
        });
    }

    @Test
    void derivedQuery_withSortParameter_sorts() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(items.findByCategory("coffee", Sort.by(Sort.Order.desc("stock")))).extracting(Item::id)
                    .containsExactly("i2", "i1");
        });
    }

    @Test
    void derivedQuery_withTop_limitsAfterSorting() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(items.findTop2ByOrderByStockDesc()).extracting(Item::id).containsExactly("i4", "i2");
            assertThat(items.findFirstByCategoryOrderByStockDesc("coffee")).get().extracting(Item::id)
                    .isEqualTo("i2");
        });
    }

    @Test
    void derivedQuery_returningOptionalOrEntity_findsTheSingleResult() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(items.findByName("Mug")).get().extracting(Item::id).isEqualTo("i4");
            assertThat(items.findByName("Unknown")).isEmpty();
            assertThat(items.findOneByName("Teapot").id()).isEqualTo("i3");
            assertThat(items.findOneByName("Unknown")).isNull();
        });
    }

    @Test
    void derivedQuery_givenSeveralResultsForASingleResult_fails() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));
            items.save(new Item("i5", "Mug", "x", 1, true, FEBRUARY, List.of()));

            assertThatThrownBy(() -> items.findByName("Mug"))
                    .isInstanceOf(IncorrectResultSizeDataAccessException.class);
        });
    }

    @Test
    void derivedQuery_returningStream_streams() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            try (var stream = items.streamByCategory("coffee")) {
                assertThat(stream.map(Item::id)).containsExactlyInAnyOrder("i1", "i2");
            }
        });
    }

    @Test
    void derivedQuery_returningPageOrSlice_pages() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            var page = items.findByActive(true, PageRequest.of(0, 2, Sort.by("stock")));
            var slice = items.findSliceByActive(true, PageRequest.of(1, 2, Sort.by("stock")));

            assertThat(page.getContent()).extracting(Item::id).containsExactly("i3", "i1");
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(slice.getContent()).extracting(Item::id).containsExactly("i4");
            assertThat(slice.hasNext()).isFalse();
        });
    }

    @Test
    void derivedQuery_countAndExists_projectTheMatches() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(items.countByCategory("coffee")).isEqualTo(2);
            assertThat(items.countByActive(true)).isEqualTo(3);
            assertThat(items.existsByCategory("tea")).isTrue();
            assertThat(items.existsByCategory("juice")).isFalse();
        });
    }

    @Test
    void derivedQuery_delete_removesTheMatches() {
        runner.withUserConfiguration(FixtureConfiguration.class).run(context -> {
            var items = filled(context.getBean(ItemRepository.class));

            assertThat(items.deleteByCategory("coffee")).isEqualTo(2);
            items.removeByName("Mug");

            assertThat(items.findAll()).extracting(Item::id).containsExactly("i3");
        });
    }

    @Test
    void derivedQuery_withUnsupportedKeyword_failsAtStartupNamingTheMethod() {
        runner.withUserConfiguration(BrokenConfiguration.class).run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .hasStackTraceContaining("GadgetRepository#findByNameNear")
                .hasStackTraceContaining("@IntegrationTest(containers = true)"));
    }

    private static ItemRepository filled(ItemRepository items) {
        items.save(new Item("i1", "Espresso Machine", "coffee", 5, true, Instant.parse("2024-01-01T00:00:00Z"),
                List.of("a", "b")));
        items.save(new Item("i2", "espresso grinder", "coffee", 10, false, FEBRUARY, List.of("b")));
        items.save(new Item("i3", "Teapot", "tea", 0, true, Instant.parse("2024-03-01T00:00:00Z"), List.of()));
        items.save(new Item("i4", "Mug", null, 20, true, Instant.parse("2024-04-01T00:00:00Z"), List.of("c")));
        return items;
    }
}
