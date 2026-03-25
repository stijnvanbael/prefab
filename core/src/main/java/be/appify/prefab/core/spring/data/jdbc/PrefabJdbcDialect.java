package be.appify.prefab.core.spring.data.jdbc;

import java.util.Collection;
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
 * JdbcDialect wrapper that enriches the default Spring Data JDBC dialect with support for Prefab single-value record
 * types. Single-value record types are Java records with exactly one component; they are treated as scalar values
 * rather than nested entities by Spring Data JDBC.
 * <p>
 * Custom converters registered via {@link org.springframework.data.jdbc.core.convert.JdbcCustomConversions} are
 * sufficient to mark single-value records as simple types in the JDBC mapping context.
 * Array support for single-value records is provided by {@link PrefabJdbcArrayColumns}.
 * </p>
 */
@Component
@Primary
public class PrefabJdbcDialect implements JdbcDialect {
    private final JdbcDialect dialect;

    /**
     * Constructs a new PrefabJdbcDialect that wraps the given JdbcDialect.
     *
     * @param dialect
     *         the JdbcDialect to wrap and enrich with Prefab-specific type support
     */
    public PrefabJdbcDialect(JdbcDialect dialect) {
        this.dialect = dialect;
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
        return dialect.simpleTypes();
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
