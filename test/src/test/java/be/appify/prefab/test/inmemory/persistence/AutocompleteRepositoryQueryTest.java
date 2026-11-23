package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.util.TypeaheadQuery;
import be.appify.prefab.test.inmemory.InMemoryPersistenceAutoConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.FixtureConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Product;
import be.appify.prefab.test.inmemory.persistence.fixture.ProductRepository;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AutocompleteRepositoryQueryTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryPersistenceAutoConfiguration.class))
            .withUserConfiguration(FixtureConfiguration.class)
            .withPropertyValues("prefab.test.containers=false");

    static Stream<Arguments> jdbcSignatures() {
        return Stream.of(
                Arguments.of("autocompletePrefixExact", "Spring", List.of("Spring Boot", "Springfield")),
                Arguments.of("autocompletePrefixExact", "spring", List.of("spring data")),
                Arguments.of("autocompletePrefixIgnoreCase", "SPRING",
                        List.of("Spring Boot", "Springfield", "spring data")),
                Arguments.of("autocompleteContainsExact", "Boot", List.of("Plain Boot", "Spring Boot")),
                Arguments.of("autocompleteContainsExact", "boot", List.of()),
                Arguments.of("autocompleteContainsIgnoreCase", "BOOT", List.of("Plain Boot", "Spring Boot")),
                Arguments.of("autocompletePrefixFuzzy", "sprng", List.of("Spring Boot", "Springfield", "spring data")),
                Arguments.of("autocompleteContainsFuzzy", "plain boo", List.of("Plain Boot")),
                Arguments.of("autocompletePrefixFullText", "spr bo", List.of("Spring Boot")),
                Arguments.of("autocompletePrefixFullText", "boo", List.of("Plain Boot", "Spring Boot")),
                Arguments.of("autocompletePrefixFullText", "sb", List.of("Spring Boot")),
                Arguments.of("autocompletePrefixFullText", "", List.of("Plain Boot", "Spring Boot", "Springfield",
                        "spring data")),
                Arguments.of("autocompleteContainsFullTextFuzzy", "sprng bo", List.of("Spring Boot")));
    }

    @ParameterizedTest(name = "{0}({1})")
    @MethodSource("jdbcSignatures")
    void autocomplete_returnsDistinctSortedMatchingValues(String method, String query, List<String> expected) {
        runner.run(context -> {
            fill(context.getBean(ProductRepository.class));

            var values = call(context.getBean(ProductRepository.class), method, query, 20, 0);

            assertThat(values).containsExactlyElementsOf(expected);
        });
    }

    @Test
    void autocomplete_appliesLimitAndOffset() {
        runner.run(context -> {
            fill(context.getBean(ProductRepository.class));

            var values = call(context.getBean(ProductRepository.class), "autocompletePrefixIgnoreCase", "spring", 2, 1);

            assertThat(values).containsExactly("Springfield", "spring data");
        });
    }

    @Test
    void autocomplete_withPageableSignature_matchesTheFullTextRegularExpression() {
        runner.run(context -> {
            var products = fill(context.getBean(ProductRepository.class));

            var values = products.autocompleteWithRegularExpression(
                    TypeaheadQuery.valueRegex("spr bo"), PageRequest.of(0, 10));

            assertThat(values).containsExactly("Spring Boot");
        });
    }

    @Test
    void autocomplete_withPageableSignature_appliesThePage() {
        runner.run(context -> {
            var products = fill(context.getBean(ProductRepository.class));

            var values = products.autocompleteWithRegularExpression("", PageRequest.of(1, 2));

            assertThat(values).containsExactly("Springfield", "spring data");
        });
    }

    @SuppressWarnings("unchecked")
    private static List<String> call(ProductRepository repository, String method, String query, int limit, long offset)
            throws ReflectiveOperationException {
        try {
            return (List<String>) ProductRepository.class.getMethod(method, String.class, int.class, long.class)
                    .invoke(repository, query, limit, offset);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Autocomplete failed", e.getCause());
        }
    }

    private static ProductRepository fill(ProductRepository products) {
        products.save(new Product("1", "Spring Boot"));
        products.save(new Product("2", "spring data"));
        products.save(new Product("3", "Plain Boot"));
        products.save(new Product("4", "Spring Boot"));
        products.save(new Product("5", "Springfield"));
        return products;
    }
}
