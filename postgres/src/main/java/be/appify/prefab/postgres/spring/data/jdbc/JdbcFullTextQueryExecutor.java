package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.annotations.rest.Filter.Weight;
import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.core.util.InitialsExpression;
import be.appify.prefab.core.util.TsVectorExpression;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.convert.EntityRowMapper;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.QueryMapper;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.relational.core.dialect.Escaper;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.sql.Comparison;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.OrderByField;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.data.relational.core.sql.SimpleFunction;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.data.relational.repository.query.RelationalExampleMapper;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * {@link FullTextQueryExecutor} for Spring Data JDBC using PostgreSQL text search.
 * <p>
 * Spring Data JDBC's {@code Criteria} cannot express the {@code @@} operator, so the example and range criteria are
 * mapped to SQL conditions with Spring Data's own {@link QueryMapper}, combined with the full-text predicate in a
 * select built with the Spring Data SQL DSL, and executed through {@link NamedParameterJdbcOperations}. The search
 * text is always bound as a parameter and interpreted by {@code websearch_to_tsquery}, which never fails on user
 * input. Results are ordered by {@code ts_rank_cd} over the same weighted tsvector expression as the generated index
 * when the sort asks for {@link FullTextCondition#RELEVANCE} or is empty. A fuzzy condition additionally matches rows
 * where every plain word of the search text has a {@code word_similarity} (pg_trgm) to a searched column that reaches
 * the similarity threshold; with type-ahead the last word may alternatively be a prefix.
 */
public class JdbcFullTextQueryExecutor implements FullTextQueryExecutor {
    private static final String BIND_NAME = "fullTextQuery";
    private static final String FUZZY_WORD_BIND_PREFIX = "fullTextFuzzyWord";
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final String WORD_PREFIX_BIND_PREFIX = "fullTextWordPrefix";
    private static final String PREFIX_BIND_NAME = "fullTextPrefix";
    private static final String INITIALS_BIND_NAME = "fullTextInitials";
    private static final String THRESHOLD_BIND_NAME = "fullTextSimilarityThreshold";

    private final NamedParameterJdbcOperations jdbc;
    private final JdbcMappingContext mappingContext;
    private final JdbcConverter converter;
    private final QueryMapper queryMapper;
    private final RelationalExampleMapper exampleMapper;
    private final SqlRenderer renderer;
    private final String configuration;
    private final Escaper likeEscaper;

    /**
     * Constructs a new JdbcFullTextQueryExecutor.
     *
     * @param jdbc
     *         the JDBC operations used to execute the query
     * @param mappingContext
     *         the mapping context used to resolve tables and columns
     * @param converter
     *         the converter used to map result rows to entities
     * @param dialect
     *         the dialect used to render the SQL
     * @param configuration
     *         the PostgreSQL text search configuration, for example {@code simple}
     */
    public JdbcFullTextQueryExecutor(
            NamedParameterJdbcOperations jdbc,
            JdbcMappingContext mappingContext,
            JdbcConverter converter,
            JdbcDialect dialect,
            String configuration
    ) {
        this.jdbc = jdbc;
        this.mappingContext = mappingContext;
        this.converter = converter;
        this.queryMapper = new QueryMapper(converter);
        this.exampleMapper = new RelationalExampleMapper(mappingContext);
        this.renderer = SqlRenderer.create(new RenderContextFactory(dialect).createRenderContext());
        this.configuration = configuration;
        this.likeEscaper = dialect.getLikeEscaper();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Page<T> findAll(
            Example<T> example,
            FullTextCondition fullText,
            List<RangeCondition> ranges,
            Pageable pageable
    ) {
        var entity = (RelationalPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(example.getProbeType());
        var table = Table.create(entity.getQualifiedTableName());
        var parameters = new MapSqlParameterSource();
        var where = whereOf(example, fullText, ranges, entity, table, parameters);
        var rows = jdbc.query(
                renderer.render(selectOf(table, where, fullText, pageable, entity)),
                parameters,
                new EntityRowMapper<>(entity, converter));
        return PageableExecutionUtils.getPage(rows, pageable, () -> count(table, where, parameters));
    }

    private Condition[] whereOf(
            Example<?> example,
            FullTextCondition fullText,
            List<RangeCondition> ranges,
            RelationalPersistentEntity<?> entity,
            Table table,
            MapSqlParameterSource parameters
    ) {
        var conditions = new ArrayList<Condition>();
        Optional.of(JdbcRangeQueryExecutor.criteriaOf(exampleMapper, example, ranges)).filter(criteria -> !criteria.isEmpty())
                .map(criteria -> ValueFunctionResolvingCriteria.wrap(criteria, likeEscaper))
                .map(criteria -> queryMapper.getMappedObject(parameters, criteria, table, entity))
                .ifPresent(conditions::add);
        if (fullText.isActive()) {
            bindSearch(fullText, parameters);
            conditions.add(matches(fullText, entity));
        }
        return conditions.toArray(Condition[]::new);
    }

    private static void bindSearch(FullTextCondition fullText, MapSqlParameterSource parameters) {
        var typeahead = fullText.typeaheadQuery();
        parameters.addValue(BIND_NAME, typeahead.wholeWords());
        parameters.addValue(THRESHOLD_BIND_NAME, fullText.similarityThreshold());
        // Prefixes and initials only hold letters and digits, so they cannot break the tsquery or the LIKE pattern
        if (!typeahead.prefixes().isEmpty()) {
            parameters.addValue(PREFIX_BIND_NAME, prefixQueryOf(typeahead.prefixes()));
        }
        typeahead.initials().ifPresent(initials -> parameters.addValue(INITIALS_BIND_NAME, initials + "%"));
        if (fullText.fuzzy()) {
            var words = fuzzyWordsOf(fullText.query());
            IntStream.range(0, words.size()).forEach(index -> {
                parameters.addValue(FUZZY_WORD_BIND_PREFIX + index, words.get(index));
                parameters.addValue(WORD_PREFIX_BIND_PREFIX + index, words.get(index) + ":*");
            });
        }
    }

    private static String prefixQueryOf(List<String> prefixes) {
        return prefixes.stream().map(prefix -> prefix + ":*").collect(Collectors.joining(" & "));
    }

    private Condition matches(FullTextCondition fullText, RelationalPersistentEntity<?> entity) {
        var alternatives = new ArrayList<Condition>();
        alternatives.add(Comparison.create(tsVector(fullText, entity), "@@", tsQuery(fullText)));
        initialsMatch(fullText, entity).ifPresent(alternatives::add);
        if (fullText.fuzzy()) {
            similarTo(fullText, entity).ifPresent(alternatives::add);
        }
        var any = alternatives.stream().reduce(Condition::or).orElseThrow();
        return alternatives.size() > 1 ? Conditions.nest(any) : any;
    }

    private Expression tsVector(FullTextCondition fullText, RelationalPersistentEntity<?> entity) {
        var weightByColumn = new LinkedHashMap<String, Weight>();
        fullText.properties().forEach(property ->
                weightByColumn.put(columnOf(entity, property), fullText.weightOf(property)));
        return Expressions.just(TsVectorExpression.of(configuration, weightByColumn));
    }

    private Expression tsQuery(FullTextCondition fullText) {
        var typeahead = fullText.typeaheadQuery();
        if (typeahead.prefixes().isEmpty()) {
            return webSearch();
        }
        var prefixes = "to_tsquery(%s, :%s)".formatted(literal(), PREFIX_BIND_NAME);
        return typeahead.wholeWords().isEmpty()
                ? Expressions.just(prefixes)
                : Expressions.just("(%s && %s)".formatted(webSearchSql(), prefixes));
    }

    private Expression webSearch() {
        return Expressions.just(webSearchSql());
    }

    private String webSearchSql() {
        return "websearch_to_tsquery(%s, :%s)".formatted(literal(), BIND_NAME);
    }

    private String literal() {
        return "'" + configuration.replace("'", "''") + "'";
    }

    // The first letters of the words of any searched column start with the search text, so "sbg" finds "Spring Boot"
    private Optional<Condition> initialsMatch(FullTextCondition fullText, RelationalPersistentEntity<?> entity) {
        return fullText.typeaheadQuery().initials().flatMap(initials -> fullText.properties().stream()
                .map(property -> (Condition) Comparison.create(
                        Expressions.just(InitialsExpression.of(columnOf(entity, property))),
                        "LIKE",
                        SQL.bindMarker(":" + INITIALS_BIND_NAME)))
                .reduce(Condition::or));
    }

    // Every word must match some searched column, so "sprint boo" finds "Spring Boot" but "spring post" does not
    private Optional<Condition> similarTo(FullTextCondition fullText, RelationalPersistentEntity<?> entity) {
        var words = fuzzyWordsOf(fullText.query());
        var typeahead = fullText.typeaheadQuery();
        return IntStream.range(0, words.size())
                .mapToObj(index -> {
                    var similar = fullText.properties().stream()
                            .map(property -> (Condition) Comparison.create(
                                    SimpleFunction.create("word_similarity", List.of(
                                            SQL.bindMarker(":" + FUZZY_WORD_BIND_PREFIX + index),
                                            Expressions.just("\"%s\"".formatted(columnOf(entity, property))))),
                                    ">=",
                                    SQL.bindMarker(":" + THRESHOLD_BIND_NAME)))
                            .reduce(Condition::or)
                            .orElseThrow();
                    var isPrefix = !typeahead.prefixes().isEmpty()
                            && (typeahead.wholeWords().isEmpty() || index == words.size() - 1);
                    return Conditions.nest(isPrefix ? similar.or(wordPrefixMatch(fullText, entity, index)) : similar);
                })
                .reduce(Condition::and);
    }

    private Condition wordPrefixMatch(FullTextCondition fullText, RelationalPersistentEntity<?> entity, int index) {
        return Comparison.create(tsVector(fullText, entity), "@@",
                Expressions.just("to_tsquery(%s, :%s)".formatted(literal(), WORD_PREFIX_BIND_PREFIX + index)));
    }

    // Words of the search text that are plain terms: not negated and not the OR operator
    private static List<String> fuzzyWordsOf(String query) {
        var words = new ArrayList<String>();
        var matcher = WORD.matcher(query);
        while (matcher.find()) {
            var negated = matcher.start() > 0 && query.charAt(matcher.start() - 1) == '-';
            if (!negated && !"OR".equalsIgnoreCase(matcher.group())) {
                words.add(matcher.group());
            }
        }
        return words;
    }

    private static String columnOf(RelationalPersistentEntity<?> entity, String property) {
        return entity.getRequiredPersistentProperty(property).getColumnName().getReference();
    }

    private Select selectOf(
            Table table,
            Condition[] where,
            FullTextCondition fullText,
            Pageable pageable,
            RelationalPersistentEntity<?> entity
    ) {
        var select = whereClause(Select.builder().select(table.asterisk()).from(table), where);
        if (pageable.isPaged()) {
            select = (SelectBuilder.SelectOrdered) ((SelectBuilder.SelectLimitOffset) select)
                    .limitOffset(pageable.getPageSize(), pageable.getOffset());
        }
        return select.orderBy(orderOf(table, fullText, pageable.getSort(), entity)).build();
    }

    private List<OrderByField> orderOf(
            Table table,
            FullTextCondition fullText,
            Sort sort,
            RelationalPersistentEntity<?> entity
    ) {
        var orders = new ArrayList<OrderByField>();
        if (fullText.ranksByRelevance(sort)) {
            var rank = SimpleFunction.create("ts_rank_cd", List.of(tsVector(fullText, entity), tsQuery(fullText)));
            orders.add(OrderByField.from(rank).desc());
        }
        orders.addAll(queryMapper.getMappedSort(table, FullTextCondition.withoutRelevance(sort), entity));
        return orders;
    }

    private long count(Table table, Condition[] where, MapSqlParameterSource parameters) {
        var select = whereClause(
                Select.builder().select(Functions.count(Expressions.just("1"))).from(table), where).build();
        return jdbc.queryForObject(renderer.render(select), parameters, Long.class);
    }

    private static SelectBuilder.SelectOrdered whereClause(SelectBuilder.SelectFromAndJoin from, Condition[] where) {
        if (where.length == 0) {
            return from;
        }
        return from.where(Arrays.stream(where).reduce(Condition::and).orElseThrow());
    }
}
