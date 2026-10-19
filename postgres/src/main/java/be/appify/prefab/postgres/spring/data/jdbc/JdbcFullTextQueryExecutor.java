package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.annotations.rest.Filter.Weight;
import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.core.util.TsVectorExpression;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
 * when the sort asks for {@link FullTextCondition#RELEVANCE} or is empty. A fuzzy condition additionally matches rows where {@code word_similarity} (pg_trgm) between the search text
 * and any searched column reaches the similarity threshold.
 */
public class JdbcFullTextQueryExecutor implements FullTextQueryExecutor {
    private static final String BIND_NAME = "fullTextQuery";
    private static final String PREFIX_BIND_NAME = "fullTextPrefix";
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
            var typeahead = fullText.typeaheadQuery();
            parameters.addValue(BIND_NAME, typeahead.wholeWords());
            typeahead.prefix().ifPresent(prefix -> parameters.addValue(PREFIX_BIND_NAME, prefix + ":*"));
            parameters.addValue(THRESHOLD_BIND_NAME, fullText.similarityThreshold());
            conditions.add(matches(fullText, entity, table));
        }
        return conditions.toArray(Condition[]::new);
    }

    private Condition matches(FullTextCondition fullText, RelationalPersistentEntity<?> entity, Table table) {
        var exact = Comparison.create(tsVector(fullText, entity), "@@", tsQuery(fullText));
        return fullText.fuzzy() ? Conditions.nest(exact.or(similarTo(fullText, entity))) : exact;
    }

    private Expression tsVector(FullTextCondition fullText, RelationalPersistentEntity<?> entity) {
        var weightByColumn = new LinkedHashMap<String, Weight>();
        fullText.properties().forEach(property ->
                weightByColumn.put(columnOf(entity, property), fullText.weightOf(property)));
        return Expressions.just(TsVectorExpression.of(configuration, weightByColumn));
    }

    private Expression tsQuery(FullTextCondition fullText) {
        var webSearch = SimpleFunction.create("websearch_to_tsquery",
                List.of(SQL.literalOf(configuration), SQL.bindMarker(":" + BIND_NAME)));
        if (fullText.typeaheadQuery().prefix().isEmpty()) {
            return webSearch;
        }
        // The prefix bind only holds letters and digits followed by :*, so to_tsquery cannot fail on it
        var literal = "'" + configuration.replace("'", "''") + "'";
        return Expressions.just("(websearch_to_tsquery(%1$s, :%2$s) && to_tsquery(%1$s, :%3$s))"
                .formatted(literal, BIND_NAME, PREFIX_BIND_NAME));
    }

    private Condition similarTo(FullTextCondition fullText, RelationalPersistentEntity<?> entity) {
        return fullText.properties().stream()
                .map(property -> (Condition) Comparison.create(
                        SimpleFunction.create("word_similarity", List.of(
                                SQL.bindMarker(":" + BIND_NAME),
                                Expressions.just("\"%s\"".formatted(columnOf(entity, property))))),
                        ">=",
                        SQL.bindMarker(":" + THRESHOLD_BIND_NAME)))
                .reduce(Condition::or)
                .orElseThrow();
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
