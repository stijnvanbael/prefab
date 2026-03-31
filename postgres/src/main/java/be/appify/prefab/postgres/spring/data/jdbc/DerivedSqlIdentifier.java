package be.appify.prefab.postgres.spring.data.jdbc;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.util.Assert;

/**
 * A simple implementation of {@link SqlIdentifier} that represents a single name part. It supports quoted identifiers and applies the
 */
public class DerivedSqlIdentifier implements SqlIdentifier {

    private final String name;
    private final boolean quoted;
    private final String toString;
    private volatile @Nullable CachedSqlName sqlName;

    DerivedSqlIdentifier(String name, boolean quoted) {

        Assert.hasText(name, "A database object must have at least on name part.");

        this.name = name;
        this.quoted = quoted;
        this.toString = quoted ? toSql(IdentifierProcessing.ANSI) : this.name;
    }

    @Override
    public Iterator<SqlIdentifier> iterator() {
        return Collections.<SqlIdentifier>singleton(this).iterator();
    }

    @Override
    public SqlIdentifier transform(UnaryOperator<String> transformationFunction) {

        Assert.notNull(transformationFunction, "Transformation function must not be null");

        return new DerivedSqlIdentifier(transformationFunction.apply(name), quoted);
    }

    @Override
    public String toSql(IdentifierProcessing processing) {

        // using a local copy of volatile this.sqlName to ensure thread safety.
        CachedSqlName sqlName = this.sqlName;
        if (sqlName == null || sqlName.processing != processing) {

            String normalized = processing.standardizeLetterCase(name);
            this.sqlName = sqlName = new CachedSqlName(processing, quoted ? processing.quote(normalized) : normalized);
            return sqlName.sqlName();
        }

        return sqlName.sqlName();
    }

    @Override
    public boolean equals(@Nullable Object o) {

        if (this == o) {
            return true;
        }

        if (o instanceof SqlIdentifier) {
            return toString().equals(o.toString());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return toString;
    }

    record CachedSqlName(IdentifierProcessing processing, String sqlName) {
    }
}
