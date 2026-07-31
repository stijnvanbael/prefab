package be.appify.prefab.postgres.spring.data.jdbc;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.relational.core.dialect.Escaper;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.ValueFunction;
import org.springframework.data.relational.core.sql.SqlIdentifier;

/**
 * Wraps a {@link CriteriaDefinition}, eagerly resolving any {@link ValueFunction} value with the given
 * {@link Escaper}.
 * <p>
 * Works around a Spring Data JDBC 4.1.0 regression (spring-projects/spring-data-relational#2317) where the
 * {@code contains}/{@code startsWith}/{@code endsWith} {@code ExampleMatcher}s and derived-query keywords produce a
 * {@link ValueFunction} value that {@code QueryMapper} binds to the JDBC statement unresolved, so the query ends up
 * comparing against the lambda's {@code toString()} instead of the intended escaped LIKE pattern.
 */
class ValueFunctionResolvingCriteria implements CriteriaDefinition {

    private final CriteriaDefinition delegate;
    private final Escaper escaper;

    private ValueFunctionResolvingCriteria(CriteriaDefinition delegate, Escaper escaper) {
        this.delegate = delegate;
        this.escaper = escaper;
    }

    static @Nullable CriteriaDefinition wrap(@Nullable CriteriaDefinition criteria, Escaper escaper) {
        return criteria == null ? null : new ValueFunctionResolvingCriteria(criteria, escaper);
    }

    @Override
    public boolean isGroup() {
        return delegate.isGroup();
    }

    @Override
    public List<CriteriaDefinition> getGroup() {
        return delegate.getGroup().stream().map(criteria -> wrap(criteria, escaper)).toList();
    }

    @Override
    public @Nullable SqlIdentifier getColumn() {
        return delegate.getColumn();
    }

    @Override
    public @Nullable Comparator getComparator() {
        return delegate.getComparator();
    }

    @Override
    public @Nullable Object getValue() {
        Object value = delegate.getValue();
        return value instanceof ValueFunction<?> valueFunction ? valueFunction.apply(escaper) : value;
    }

    @Override
    public boolean isIgnoreCase() {
        return delegate.isIgnoreCase();
    }

    @Override
    public @Nullable CriteriaDefinition getPrevious() {
        return wrap(delegate.getPrevious(), escaper);
    }

    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Combinator getCombinator() {
        return delegate.getCombinator();
    }
}
