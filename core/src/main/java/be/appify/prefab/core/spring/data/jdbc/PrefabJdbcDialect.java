package be.appify.prefab.core.spring.data.jdbc;

import be.appify.prefab.core.service.SingleValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.core.dialect.JdbcArrayColumns;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.relational.core.dialect.Escaper;
import org.springframework.data.relational.core.dialect.IdGeneration;
import org.springframework.data.relational.core.dialect.InsertRenderContext;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.dialect.OrderByNullPrecedence;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SimpleFunction;
import org.springframework.data.relational.core.sql.render.SelectRenderContext;
import org.springframework.stereotype.Component;

/**
 * JdbcDialect wrapper that enriches the default Spring Data JDBC dialect with support for Prefab single-value types
 * (types annotated with {@link SingleValue}). This allows Spring Data JDBC to properly handle these types when
 * generating SQL queries and mapping results.
 * <p>
 * Types annotated with {@link SingleValue} are registered as "simple types" in the JDBC mapping context, preventing
 * Spring Data JDBC from attempting to map them as nested entities.
 * </p>
 */
@Component
@Primary
public class PrefabJdbcDialect implements JdbcDialect {
    private final JdbcDialect dialect;
    private final List<Class<?>> singleValueTypes;

    /**
     * Constructs a new PrefabJdbcDialect that wraps the given JdbcDialect and registers the provided single-value types.
     *
     * @param dialect
     *         the JdbcDialect to wrap and enrich with Prefab-specific type support
     * @param singleValueTypes
     *         additional single-value type classes to register as simple types
     */
    public PrefabJdbcDialect(JdbcDialect dialect, List<SingleValueTypeRegistrar> singleValueTypes) {
        this.dialect = dialect;
        this.singleValueTypes = singleValueTypes.stream()
                .flatMap(registrar -> registrar.singleValueTypes().stream())
                .toList();
    }

    @Override
    public LimitClause limit() {
        return dialect.limit();
    }

    @Override
    public LockClause lock() {
        return dialect.lock();
    }

    @Override
    public SelectRenderContext getSelectContext() {
        return dialect.getSelectContext();
    }

    @Override
    public JdbcArrayColumns getArraySupport() {
        return new PrefabJdbcArrayColumns(dialect.getArraySupport());
    }

    @Override
    public IdentifierProcessing getIdentifierProcessing() {
        return dialect.getIdentifierProcessing();
    }

    @Override
    public Escaper getLikeEscaper() {
        return dialect.getLikeEscaper();
    }

    @Override
    public IdGeneration getIdGeneration() {
        return dialect.getIdGeneration();
    }

    @Override
    public Collection<Object> getConverters() {
        return dialect.getConverters();
    }

    @Override
    public Set<Class<?>> simpleTypes() {
        var types = new HashSet<>(dialect.simpleTypes());
        types.addAll(singleValueTypes);
        return types;
    }

    @Override
    public InsertRenderContext getInsertRenderContext() {
        return dialect.getInsertRenderContext();
    }

    @Override
    public OrderByNullPrecedence orderByNullHandling() {
        return dialect.orderByNullHandling();
    }

    @Override
    public SimpleFunction getExistsFunction() {
        return dialect.getExistsFunction();
    }

    @Override
    public boolean supportsSingleQueryLoading() {
        return dialect.supportsSingleQueryLoading();
    }
}
