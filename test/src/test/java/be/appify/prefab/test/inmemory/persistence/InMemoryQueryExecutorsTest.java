package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.annotations.rest.Filter.Operator;
import be.appify.prefab.core.annotations.rest.Filter.Weight;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import be.appify.prefab.test.inmemory.InMemoryPersistenceAutoConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Customer;
import be.appify.prefab.test.inmemory.persistence.fixture.FixtureConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Order;
import be.appify.prefab.test.inmemory.persistence.fixture.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryQueryExecutorsTest {
    private static final Reference<Customer> ALICE = Reference.fromId("alice");
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 10, Sort.by("id"));
    private static final Example<Order> EVERYTHING = Example.of(
            new Order(null, 0, null, null, null, null, BigDecimal.ZERO, List.of()),
            ExampleMatcher.matchingAll().withIgnorePaths(
                    "id", "version", "name", "description", "category", "customer", "amount", "tags"));

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryPersistenceAutoConfiguration.class))
            .withUserConfiguration(FixtureConfiguration.class)
            .withPropertyValues("prefab.test.containers=false");

    static Stream<Arguments> ranges() {
        return Stream.of(
                Arguments.of(Operator.GREATER_THAN, "20", List.of("o3", "o4")),
                Arguments.of(Operator.GREATER_THAN_OR_EQUAL, "20", List.of("o2", "o3", "o4")),
                Arguments.of(Operator.LESS_THAN, "20", List.of("o1")),
                Arguments.of(Operator.LESS_THAN_OR_EQUAL, "20", List.of("o1", "o2")));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ranges")
    void rangeQuery_comparesNumbersByValue(Operator operator, String bound, List<String> expectedIds) {
        runner.run(context -> {
            fill(context);

            var page = context.getBean(RangeQueryExecutor.class).findAll(
                    EVERYTHING, List.of(new RangeCondition("amount", operator, new BigDecimal(bound))), FIRST_PAGE);

            assertThat(page.getContent()).extracting(Order::id).containsExactlyElementsOf(expectedIds);
            assertThat(page.getTotalElements()).isEqualTo(expectedIds.size());
        });
    }

    @Test
    void rangeQuery_withLowerAndUpperBound_selectsTheInterval() {
        runner.run(context -> {
            fill(context);

            var page = context.getBean(RangeQueryExecutor.class).findAll(EVERYTHING, List.of(
                    new RangeCondition("amount", Operator.GREATER_THAN, new BigDecimal("10")),
                    new RangeCondition("amount", Operator.LESS_THAN, new BigDecimal("40"))), FIRST_PAGE);

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o2", "o3");
        });
    }

    @Test
    void rangeQuery_withoutValue_isIgnored() {
        runner.run(context -> {
            fill(context);

            var page = context.getBean(RangeQueryExecutor.class).findAll(
                    EVERYTHING, List.of(new RangeCondition("amount", Operator.GREATER_THAN, null)), FIRST_PAGE);

            assertThat(page.getTotalElements()).isEqualTo(4);
        });
    }

    @Test
    void rangeQuery_pagesAndSorts() {
        runner.run(context -> {
            fill(context);

            var page = context.getBean(RangeQueryExecutor.class).findAll(
                    EVERYTHING, List.of(), PageRequest.of(1, 3, Sort.by(Sort.Order.desc("amount"))));

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o1");
            assertThat(page.getTotalElements()).isEqualTo(4);
        });
    }

    static Stream<Arguments> searches() {
        return Stream.of(
                Arguments.of("whole word", "grinder", false, List.of("o1")),
                Arguments.of("every word is required", "coffee grinder", false, List.of("o1")),
                Arguments.of("a partial word is no match", "grind", false, List.of()),
                Arguments.of("case insensitive", "COFFEE", false, List.of("o1", "o2")),
                Arguments.of("the description is searched too", "burr", false, List.of("o1")),
                Arguments.of("excluded word", "coffee -grinder", false, List.of("o2")),
                Arguments.of("alternatives", "grinder or mug", false, List.of("o1", "o3")),
                Arguments.of("phrase", "\"coffee maker\"", false, List.of("o2")),
                Arguments.of("phrase in the wrong order", "\"maker coffee\"", false, List.of()),
                Arguments.of("type-ahead prefix", "grin", true, List.of("o1")),
                Arguments.of("type-ahead prefixes in any order", "mak cof", true, List.of("o2")),
                Arguments.of("type-ahead initials", "cbg", true, List.of("o1")),
                Arguments.of("type-ahead completes the last word", "coffee mak", true, List.of("o2")));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("searches")
    void fullText_matchesWordsOfTheSearchedProperties(
            String scenario,
            String query,
            boolean typeahead,
            List<String> expectedIds
    ) {
        runner.run(context -> {
            fill(context);
            var condition = new FullTextCondition(List.of("name", "description"), query);

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING, typeahead ? condition.withTypeahead() : condition, List.of(), FIRST_PAGE);

            assertThat(page.getContent()).extracting(Order::id).containsExactlyElementsOf(expectedIds);
        });
    }

    @Test
    void fullText_withFuzzy_toleratesTypos() {
        runner.run(context -> {
            fill(context);
            var condition = new FullTextCondition(List.of("name", "description"), "grindr", true, 0.3);

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING, condition, List.of(), FIRST_PAGE);

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o1");
        });
    }

    @Test
    void fullText_withBlankQuery_matchesEverything() {
        runner.run(context -> {
            fill(context);

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING, new FullTextCondition(List.of("name"), " "), List.of(), FIRST_PAGE);

            assertThat(page.getTotalElements()).isEqualTo(4);
        });
    }

    @Test
    void fullText_combinesWithRangeConditions() {
        runner.run(context -> {
            fill(context);

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING,
                    new FullTextCondition(List.of("name", "description"), "coffee"),
                    List.of(new RangeCondition("amount", Operator.GREATER_THAN, new BigDecimal("15"))),
                    FIRST_PAGE);

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o2");
        });
    }

    @Test
    void fullText_withoutSort_ranksHeavierPropertiesFirst() {
        runner.run(context -> {
            fill(context);
            var condition = new FullTextCondition(List.of("name", "description"), "coffee", false, 0.3,
                    Map.of("name", Weight.A));

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING, condition, List.of(), PageRequest.of(0, 10));

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o2", "o1");
        });
    }

    @Test
    void fullText_withRelevanceInTheSort_ranksFirstAndSortsTheTies() {
        runner.run(context -> {
            fill(context);
            var condition = new FullTextCondition(List.of("name", "description"), "coffee");

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING, condition, List.of(),
                    PageRequest.of(0, 10, Sort.by(FullTextCondition.RELEVANCE).and(Sort.by(Sort.Order.desc("id")))));

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o2", "o1");
        });
    }

    @Test
    void fullText_withExplicitSort_ignoresRelevance() {
        runner.run(context -> {
            fill(context);
            var condition = new FullTextCondition(List.of("name", "description"), "coffee", false, 0.3,
                    Map.of("name", Weight.A));

            var page = context.getBean(FullTextQueryExecutor.class).findAll(
                    EVERYTHING, condition, List.of(), PageRequest.of(0, 10, Sort.by("id")));

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o1", "o2");
        });
    }

    private static void fill(ApplicationContext context) {
        var orders = context.getBean(OrderRepository.class);
        orders.save(withText(Order.create("o1", "Grinder", "tools", ALICE, "10"), "Coffee burr grinder"));
        orders.save(withText(Order.create("o2", "Coffee maker", "tools", ALICE, "20"), "Drip"));
        orders.save(withText(Order.create("o3", "Mug", "tools", ALICE, "30"), "Ceramic"));
        orders.save(withText(Order.create("o4", "Kettle", "tools", ALICE, "40"), null));
    }

    private static Order withText(Order order, String description) {
        return order.withDescription(description);
    }
}
