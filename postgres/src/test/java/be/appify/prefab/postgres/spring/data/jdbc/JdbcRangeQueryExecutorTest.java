package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.rest.Filter.Operator;
import be.appify.prefab.core.spring.data.RangeCondition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = JdbcRangeQueryExecutorTest.TestConfig.class)
class JdbcRangeQueryExecutorTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    record Item(@Id Long id, String name, BigDecimal price, int quantity, LocalDate due) {
    }

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    DataAccessStrategy dataAccessStrategy;
    @Autowired
    JdbcConverter jdbcConverter;
    @Autowired
    JdbcDialect dialect;
    @Autowired
    JdbcMappingContext mappingContext;

    JdbcAggregateOperations operations;
    @Autowired
    JdbcTemplate jdbcTemplate;

    JdbcRangeQueryExecutor executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS item");
        jdbcTemplate.execute("""
                CREATE TABLE item (id BIGSERIAL PRIMARY KEY, name TEXT, price NUMERIC, quantity INT, due DATE)""");
        var strategy = new PrefabDataAccessStrategy(dataAccessStrategy, dialect.getLikeEscaper());
        operations = new PrefabJdbcAggregateTemplate(applicationContext, mappingContext, jdbcConverter, strategy);
        executor = new JdbcRangeQueryExecutor(operations, mappingContext);
        save("apple", "10.00", 1, "2026-02-01");
        save("apricot", "20.00", 5, "2026-03-01");
        save("banana", "30.00", 10, "2026-04-01");
        save("blueberry", "40.00", 20, "2026-05-01");
    }

    private void save(String name, String price, int quantity, String due) {
        operations.insert(new Item(null, name, new BigDecimal(price), quantity, LocalDate.parse(due)));
    }

    private Example<Item> example(String name) {
        var probe = new Item(null, name, null, 0, null);
        var matcher = ExampleMatcher.matchingAll()
                .withIgnorePaths("id", "price", "quantity", "due")
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.ignoreCase().contains());
        return Example.of(probe, matcher);
    }

    private List<String> names(Example<Item> example, Pageable pageable, RangeCondition... ranges) {
        return executor.findAll(example, List.of(ranges), pageable).getContent().stream()
                .map(Item::name)
                .sorted()
                .toList();
    }

    private List<String> names(String nameFilter, RangeCondition... ranges) {
        return names(example(nameFilter), Pageable.unpaged(), ranges);
    }

    private static RangeCondition range(String property, Operator operator, Object value) {
        return new RangeCondition(property, operator, value);
    }

    @Test
    void noRangesAndNoExampleValuesReturnsEverything() {
        assertThat(names((String) null)).containsExactly("apple", "apricot", "banana", "blueberry");
    }

    @Test
    void exampleOnlyBehavesLikeTheRepository() {
        assertThat(names("ap")).containsExactly("apple", "apricot");
    }

    @Test
    void rangeOnlyWithoutExampleValues() {
        assertThat(names((String) null, range("price", Operator.GREATER_THAN, new BigDecimal("20.00"))))
                .containsExactly("banana", "blueberry");
    }

    @Test
    void greaterThanIsExclusiveAndGreaterThanOrEqualIsInclusive() {
        assertThat(names((String) null, range("quantity", Operator.GREATER_THAN, 5)))
                .containsExactly("banana", "blueberry");
        assertThat(names((String) null, range("quantity", Operator.GREATER_THAN_OR_EQUAL, 5)))
                .containsExactly("apricot", "banana", "blueberry");
    }

    @Test
    void lessThanIsExclusiveAndLessThanOrEqualIsInclusive() {
        assertThat(names((String) null, range("quantity", Operator.LESS_THAN, 5))).containsExactly("apple");
        assertThat(names((String) null, range("quantity", Operator.LESS_THAN_OR_EQUAL, 5)))
                .containsExactly("apple", "apricot");
    }

    @Test
    void bothBoundsOnTheSamePropertyActAsBetween() {
        assertThat(names((String) null,
                range("price", Operator.GREATER_THAN_OR_EQUAL, new BigDecimal("20.00")),
                range("price", Operator.LESS_THAN_OR_EQUAL, new BigDecimal("30.00"))))
                .containsExactly("apricot", "banana");
    }

    @Test
    void rangeOnDatesAndMultiplePropertiesAreCombinedWithAnd() {
        assertThat(names((String) null,
                range("due", Operator.GREATER_THAN, LocalDate.parse("2026-02-01")),
                range("due", Operator.LESS_THAN, LocalDate.parse("2026-05-01")),
                range("quantity", Operator.GREATER_THAN_OR_EQUAL, 10)))
                .containsExactly("banana");
    }

    @Test
    void rangeIsCombinedWithExampleMatcher() {
        assertThat(names("ap", range("price", Operator.GREATER_THAN, new BigDecimal("10.00"))))
                .containsExactly("apricot");
        assertThat(names("b", range("price", Operator.LESS_THAN, new BigDecimal("30.00")))).isEmpty();
    }

    @Test
    void conditionsWithoutValueAreIgnored() {
        assertThat(names("ap", range("price", Operator.GREATER_THAN, null)))
                .containsExactly("apple", "apricot");
    }

    @Test
    void emptyRangeReturnsNothing() {
        assertThat(names((String) null,
                range("price", Operator.GREATER_THAN, new BigDecimal("30.00")),
                range("price", Operator.LESS_THAN, new BigDecimal("20.00")))).isEmpty();
    }

    @Test
    void pagingSortingAndTotalCountApplyToTheCombinedQuery() {
        var sort = Sort.by("price").descending();
        var ranges = List.of(range("price", Operator.GREATER_THAN_OR_EQUAL, new BigDecimal("20.00")));

        var first = executor.findAll(example(null), ranges, PageRequest.of(0, 2, sort));
        var second = executor.findAll(example(null), ranges, PageRequest.of(1, 2, sort));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(Item::name).containsExactly("blueberry", "banana");
        assertThat(second.getContent()).extracting(Item::name).containsExactly("apricot");
    }

    @Test
    void nonRangeOperatorIsRejected() {
        assertThatThrownBy(() -> range("price", Operator.CONTAINS, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
