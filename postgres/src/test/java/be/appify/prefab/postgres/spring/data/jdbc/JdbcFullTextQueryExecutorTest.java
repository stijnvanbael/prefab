package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.rest.Filter.Operator;
import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.spring.data.RangeCondition;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JdbcFullTextQueryExecutorTest.TestConfig.class)
class JdbcFullTextQueryExecutorTest {

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

    record Article(@Id Long id, String title, String body, String category, BigDecimal price) {
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
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    NamedParameterJdbcOperations namedJdbc;

    JdbcAggregateOperations operations;
    JdbcFullTextQueryExecutor executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        jdbcTemplate.execute("DROP TABLE IF EXISTS article");
        jdbcTemplate.execute("""
                CREATE TABLE article (
                    id BIGSERIAL PRIMARY KEY, title TEXT, body TEXT, category TEXT, price NUMERIC)""");
        jdbcTemplate.execute("""
                CREATE INDEX article_title_body_fts ON article USING GIN (
                    to_tsvector('simple', coalesce("title", '') || ' ' || coalesce("body", '')))""");
        var strategy = new PrefabDataAccessStrategy(dataAccessStrategy, dialect.getLikeEscaper());
        operations = new PrefabJdbcAggregateTemplate(applicationContext, mappingContext, jdbcConverter, strategy);
        executor = new JdbcFullTextQueryExecutor(namedJdbc, mappingContext, jdbcConverter, dialect, "simple");
        save("Spring Boot guide", "Getting started with the framework", "java", "10.00");
        save("Postgres tuning", "Indexes and query plans for Spring users", "database", "20.00");
        save("Kafka basics", null, "messaging", "30.00");
        save("Mongo notes", "Document database tips", "database", "40.00");
    }

    private void save(String title, String body, String category, String price) {
        operations.insert(new Article(null, title, body, category, new BigDecimal(price)));
    }

    private static Example<Article> example(String category) {
        var matcher = ExampleMatcher.matchingAll()
                .withIgnorePaths("id", "title", "body", "price")
                .withMatcher("category", ExampleMatcher.GenericPropertyMatchers.ignoreCase().contains());
        return Example.of(new Article(null, null, null, category, null), matcher);
    }

    private List<String> titles(String query, String category, Pageable pageable, RangeCondition... ranges) {
        return executor.findAll(example(category), fullText(query), List.of(ranges), pageable)
                .getContent().stream().map(Article::title).sorted().toList();
    }

    private static FullTextCondition fullText(String query) {
        return new FullTextCondition(List.of("title", "body"), query);
    }

    private List<String> titles(String query) {
        return titles(query, null, Pageable.unpaged());
    }

    @Test
    void matchesAnyOfTheSearchedProperties() {
        assertThat(titles("kafka")).containsExactly("Kafka basics");
        assertThat(titles("framework")).containsExactly("Spring Boot guide");
        assertThat(titles("spring")).containsExactly("Postgres tuning", "Spring Boot guide");
    }

    @Test
    void ignoresCaseAndHandlesNullColumns() {
        assertThat(titles("KAFKA")).containsExactly("Kafka basics");
    }

    @Test
    void multipleTermsMustAllMatch() {
        assertThat(titles("spring postgres")).containsExactly("Postgres tuning");
    }

    @Test
    void websearchSyntaxIsSupportedAndNeverFails() {
        assertThat(titles("spring -postgres")).containsExactly("Spring Boot guide");
        assertThat(titles("spring OR mongo")).containsExactly("Mongo notes", "Postgres tuning", "Spring Boot guide");
        assertThat(titles("\"query plans\"")).containsExactly("Postgres tuning");
        assertThat(titles("'; DROP TABLE article; --")).isEmpty();
        assertThat(titles("(((")).isEmpty();
    }

    @Test
    void blankOrMissingQueryIsIgnored() {
        assertThat(titles(null)).hasSize(4);
        assertThat(titles("  ")).hasSize(4);
    }

    @Test
    void isCombinedWithExampleAndRangeConditions() {
        assertThat(titles("spring", "DATAB", Pageable.unpaged())).containsExactly("Postgres tuning");
        assertThat(titles("database", null, Pageable.unpaged(),
                new RangeCondition("price", Operator.GREATER_THAN, new BigDecimal("25.00"))))
                .containsExactly("Mongo notes");
        assertThat(titles(null, "database", Pageable.unpaged(),
                new RangeCondition("price", Operator.LESS_THAN_OR_EQUAL, new BigDecimal("20.00"))))
                .containsExactly("Postgres tuning");
    }

    @Test
    void pagingSortingAndTotalCountApplyToTheCombinedQuery() {
        var sort = Sort.by("title").descending();
        var first = executor.findAll(example(null), fullText("spring database"), List.of(), PageRequest.of(0, 1, sort));
        var all = executor.findAll(example(null), fullText("spring OR database"), List.of(), PageRequest.of(0, 2, sort));
        var second = executor.findAll(example(null), fullText("spring OR database"), List.of(), PageRequest.of(1, 2, sort));

        assertThat(first.getTotalElements()).isEqualTo(0);
        assertThat(all.getTotalElements()).isEqualTo(3);
        assertThat(all.getContent()).extracting(Article::title).containsExactly("Spring Boot guide", "Postgres tuning");
        assertThat(second.getContent()).extracting(Article::title).containsExactly("Mongo notes");
    }

    private List<String> fuzzyTitles(String query, double threshold, String category, RangeCondition... ranges) {
        var condition = new FullTextCondition(List.of("title", "body"), query, true, threshold);
        return executor.findAll(example(category), condition, List.of(ranges), Pageable.unpaged())
                .getContent().stream().map(Article::title).sorted().toList();
    }

    @Test
    void exactSearchDoesNotTolerateTypos() {
        assertThat(titles("kafak")).isEmpty();
    }

    @Test
    void fuzzySearchToleratesTypos() {
        assertThat(fuzzyTitles("kafak", 0.3, null)).containsExactly("Kafka basics");
        assertThat(fuzzyTitles("postgers", 0.3, null)).containsExactly("Postgres tuning");
        assertThat(fuzzyTitles("framwork", 0.3, null)).containsExactly("Spring Boot guide");
    }

    @Test
    void fuzzySearchStillFindsExactMatchesAndWebSearchSyntax() {
        assertThat(fuzzyTitles("kafka", 0.3, null)).containsExactly("Kafka basics");
        assertThat(fuzzyTitles("\"query plans\"", 0.9, null)).containsExactly("Postgres tuning");
    }

    @Test
    void higherSimilarityThresholdIsStricter() {
        assertThat(fuzzyTitles("kafak", 0.3, null)).isNotEmpty();
        assertThat(fuzzyTitles("kafak", 1.0, null)).isEmpty();
    }

    @Test
    void fuzzySearchCombinesWithExampleAndRangeConditions() {
        assertThat(fuzzyTitles("postgers", 0.3, "database")).containsExactly("Postgres tuning");
        assertThat(fuzzyTitles("postgers", 0.3, "java")).isEmpty();
        assertThat(fuzzyTitles("postgers", 0.3, null,
                new RangeCondition("price", Operator.GREATER_THAN, new BigDecimal("25.00")))).isEmpty();
    }}
