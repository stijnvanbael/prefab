package be.appify.prefab.test.inmemory.persistence;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.ClassUtils;

/**
 * Executes a derived query method, such as {@code findByCustomerOrderByNameAsc}, in memory. Supports the subjects
 * {@code find}, {@code read}, {@code get}, {@code query}, {@code count}, {@code exists} and {@code delete} with
 * {@code Distinct}, {@code Top} and {@code First}, the keywords listed in {@link PartMatcher}, {@code OrderBy},
 * {@code Pageable} and {@code Sort} parameters and the usual return types.
 */
final class DerivedRepositoryQuery implements RepositoryQuery {
    private final QueryMethod queryMethod;
    private final Class<?> returnType;
    private final Class<?> domainType;
    private final InMemoryStore store;
    private final PartTree tree;
    private final List<List<PartMatcher>> alternatives = new ArrayList<>();

    DerivedRepositoryQuery(QueryMethod queryMethod, Method method, Class<?> domainType, InMemoryStore store) {
        this.queryMethod = queryMethod;
        this.returnType = method.getReturnType();
        this.domainType = domainType;
        this.store = store;
        this.tree = new PartTree(method.getName(), domainType);
        var description = "%s#%s".formatted(method.getDeclaringClass().getSimpleName(), method.getName());
        tree.forEach(or -> {
            var parts = new ArrayList<PartMatcher>();
            or.forEach(part -> parts.add(new PartMatcher(part, description, store.properties())));
            alternatives.add(parts);
        });
    }

    @Override
    public Object execute(Object[] parameters) {
        var accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
        var matching = store.findAll(domainType).stream().<Object>map(row -> row).filter(predicateOf(accessor));
        var rows = (tree.isDistinct() ? matching.distinct() : matching).toList();
        if (tree.isCountProjection()) {
            return number(rows.size());
        }
        if (tree.isExistsProjection()) {
            return !rows.isEmpty();
        }
        if (tree.isDelete()) {
            return delete(rows);
        }
        var sort = tree.getSort().and(accessor.getSort());
        Comparator<Object> order = store.properties().comparator(sort);
        var pageable = accessor.getPageable();
        var limited = limited(rows, order);
        if (queryMethod.isPageQuery() || queryMethod.isSliceQuery()) {
            return Pages.of(limited, pageable, order);
        }
        return shaped(pageable.isPaged() ? Pages.of(limited, pageable, order).getContent() : sorted(limited, order));
    }

    private Predicate<Object> predicateOf(ParametersParameterAccessor accessor) {
        var values = new ArrayList<Object>();
        accessor.forEach(values::add);
        var iterator = values.iterator();
        var alternativePredicates = new ArrayList<Predicate<Object>>();
        for (var parts : alternatives) {
            Predicate<Object> all = candidate -> true;
            for (var part : parts) {
                all = all.and(part.bind(iterator));
            }
            alternativePredicates.add(all);
        }
        return alternativePredicates.stream().reduce(Predicate::or).orElse(candidate -> true);
    }

    private List<Object> limited(List<Object> rows, Comparator<Object> order) {
        return tree.isLimiting() ? sorted(rows, order).stream().limit(tree.getMaxResults()).toList() : rows;
    }

    private static List<Object> sorted(List<Object> rows, Comparator<Object> order) {
        return rows.stream().sorted(order).toList();
    }

    private Object delete(List<Object> rows) {
        rows.forEach(store::delete);
        if (Collection.class.isAssignableFrom(returnType)) {
            return rows;
        }
        return Number.class.isAssignableFrom(ClassUtils.resolvePrimitiveIfNecessary(returnType))
                ? number(rows.size()) : List.of();
    }

    private Object shaped(List<Object> rows) {
        if (Optional.class.equals(returnType)) {
            return single(rows);
        }
        if (Stream.class.equals(returnType)) {
            return rows.stream();
        }
        if (Iterable.class.isAssignableFrom(returnType)) {
            return rows;
        }
        return single(rows).orElse(null);
    }

    private Optional<Object> single(List<Object> rows) {
        if (rows.size() > 1 && !tree.isLimiting()) {
            throw new IncorrectResultSizeDataAccessException(1, rows.size());
        }
        return rows.stream().findFirst();
    }

    private Object number(int count) {
        return int.class.equals(returnType) || Integer.class.equals(returnType) ? (Object) count : (Object) (long) count;
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}
