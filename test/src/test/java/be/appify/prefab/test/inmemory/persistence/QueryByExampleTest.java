package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.inmemory.InMemoryPersistenceAutoConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Customer;
import be.appify.prefab.test.inmemory.persistence.fixture.FixtureConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Order;
import be.appify.prefab.test.inmemory.persistence.fixture.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class QueryByExampleTest {
    private static final Reference<Customer> ALICE = Reference.fromId("alice");
    private static final Reference<Customer> BOB = Reference.fromId("bob");
    private static final List<String> ALL_FIELDS = List.of(
            "id", "version", "name", "description", "category", "customer", "amount", "tags");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryPersistenceAutoConfiguration.class))
            .withUserConfiguration(FixtureConfiguration.class)
            .withPropertyValues("prefab.test.containers=false");

    static Stream<Arguments> stringMatchers() {
        return Stream.of(
                Arguments.of("exact", "Spring Boot", GenericPropertyMatchers.caseSensitive().exact(),
                        List.of("o1")),
                Arguments.of("exact is case sensitive", "spring boot", GenericPropertyMatchers.caseSensitive().exact(),
                        List.of()),
                Arguments.of("exact ignoring case", "spring boot", GenericPropertyMatchers.ignoreCase().exact(),
                        List.of("o1")),
                Arguments.of("contains", "Boot", GenericPropertyMatchers.caseSensitive().contains(),
                        List.of("o1", "o2")),
                Arguments.of("contains ignoring case", "boot", GenericPropertyMatchers.ignoreCase().contains(),
                        List.of("o1", "o2")),
                Arguments.of("starts with", "Spring", GenericPropertyMatchers.caseSensitive().startsWith(),
                        List.of("o1", "o3")),
                Arguments.of("starts with ignoring case", "SPRING", GenericPropertyMatchers.ignoreCase().startsWith(),
                        List.of("o1", "o3")),
                Arguments.of("ends with", "Boot", GenericPropertyMatchers.caseSensitive().endsWith(),
                        List.of("o1", "o2")),
                Arguments.of("regex", "^Spring.*(Boot|Data)$", GenericPropertyMatchers.caseSensitive().regex(),
                        List.of("o1", "o3")),
                Arguments.of("regex ignoring case", "boot$", GenericPropertyMatchers.ignoreCase().regex(),
                        List.of("o1", "o2")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stringMatchers")
    void findAll_appliesTheStringMatcherOfTheProperty(
            String scenario,
            String name,
            ExampleMatcher.GenericPropertyMatcher matcher,
            List<String> expectedIds
    ) {
        runner.run(context -> {
            var orders = filled(context.getBean(OrderRepository.class));
            var probe = probe(name, null, null);
            var example = Example.of(probe, ignoringAllBut(ExampleMatcher.matchingAll(), "name")
                    .withMatcher("name", matcher));

            var page = orders.findAll(example, PageRequest.of(0, 10, Sort.by("id")));

            assertThat(page.getContent()).extracting(Order::id).containsExactlyElementsOf(expectedIds);
            assertThat(page.getTotalElements()).isEqualTo(expectedIds.size());
        });
    }

    @Test
    void findAll_combinesAllNonIgnoredProperties() {
        runner.run(context -> {
            var orders = filled(context.getBean(OrderRepository.class));
            var example = Example.of(probe("Spring", "tools", ALICE),
                    ignoringAllBut(ExampleMatcher.matchingAll(), "name", "category", "customer")
                            .withMatcher("name", GenericPropertyMatchers.startsWith()));

            var page = orders.findAll(example, PageRequest.of(0, 10, Sort.by("id")));

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o1");
        });
    }

    @Test
    void findAll_withIgnoredPaths_ignoresThePropertyOfTheProbe() {
        runner.run(context -> {
            var orders = filled(context.getBean(OrderRepository.class));
            var example = Example.of(probe("Spring Boot", "unrelated", null),
                    ignoringAllBut(ExampleMatcher.matchingAll(), "name"));

            assertThat(orders.findAll(example, PageRequest.of(0, 10)).getContent()).extracting(Order::id)
                    .containsExactly("o1");
        });
    }

    @Test
    void findAll_matchingAny_acceptsOneMatchingProperty() {
        runner.run(context -> {
            var orders = filled(context.getBean(OrderRepository.class));
            var example = Example.of(probe("Spring Data", "kitchen", null),
                    ignoringAllBut(ExampleMatcher.matchingAny(), "name", "category"));

            assertThat(orders.findAll(example, PageRequest.of(0, 10, Sort.by("id"))).getContent())
                    .extracting(Order::id).containsExactly("o2", "o3", "o4");
        });
    }

    @Test
    void findAll_withReference_comparesTheReference() {
        runner.run(context -> {
            var orders = filled(context.getBean(OrderRepository.class));
            var example = Example.of(probe(null, null, BOB),
                    ignoringAllBut(ExampleMatcher.matchingAll(), "customer"));

            assertThat(orders.findAll(example, PageRequest.of(0, 10, Sort.by("id"))).getContent())
                    .extracting(Order::id).containsExactly("o3", "o4");
        });
    }

    @Test
    void findAll_pagesAndSortsTheMatches() {
        runner.run(context -> {
            var orders = filled(context.getBean(OrderRepository.class));
            var example = Example.of(probe("o", null, null),
                    ignoringAllBut(ExampleMatcher.matchingAll(), "name")
                            .withMatcher("name", GenericPropertyMatchers.ignoreCase().contains()));

            var page = orders.findAll(example, PageRequest.of(1, 2, Sort.by(Sort.Order.desc("name"))));

            assertThat(page.getContent()).extracting(Order::name).containsExactly("Other");
            assertThat(page.getTotalElements()).isEqualTo(3);
        });
    }

    private static ExampleMatcher ignoringAllBut(ExampleMatcher matcher, String... kept) {
        var keptFields = List.of(kept);
        var ignored = ALL_FIELDS.stream().filter(field -> !keptFields.contains(field)).toArray(String[]::new);
        return matcher.withIgnorePaths(ignored);
    }

    private static Order probe(String name, String category, Reference<Customer> customer) {
        return new Order(null, 0, name, null, category, customer, BigDecimal.ZERO, List.of());
    }

    private static OrderRepository filled(OrderRepository orders) {
        orders.save(Order.create("o1", "Spring Boot", "tools", ALICE, "10"));
        orders.save(Order.create("o2", "Plain Boot", "kitchen", ALICE, "20"));
        orders.save(Order.create("o3", "Spring Data", "tools", BOB, "30"));
        orders.save(Order.create("o4", "Other", "kitchen", BOB, "40"));
        return orders;
    }
}
