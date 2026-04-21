package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.util.IdentifierShortener;
import java.util.Iterator;
import static be.appify.prefab.core.util.IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH;
import java.util.function.UnaryOperator;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;

/**
 * A {@link SqlIdentifier} decorator that deduplicates repeated snake_case segments after a prefix transformation
 * is applied. This ensures that embedded property paths like {@code person_name_first_name} are automatically
 * shortened to {@code person_name_first} when Spring Data JDBC prepends the embedded prefix.
 * <p>
 * For non-embedded properties no transformation is applied and this identifier behaves identically to its delegate.
 * </p>
 */
class DeduplicatingSqlIdentifier implements SqlIdentifier {

    private final SqlIdentifier delegate;

    DeduplicatingSqlIdentifier(SqlIdentifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public SqlIdentifier transform(UnaryOperator<String> transformationFunction) {
        var transformed = transformationFunction.apply(delegate.toSql(IdentifierProcessing.NONE));
        var shortened = IdentifierShortener.shorten(IdentifierShortener.deduplicate(transformed), POSTGRES_MAX_IDENTIFIER_LENGTH);
        return new DeduplicatingSqlIdentifier(SqlIdentifier.quoted(shortened));
    }

    @Override
    public String toSql(IdentifierProcessing processing) {
        return delegate.toSql(processing);
    }

    @Override
    public Iterator<SqlIdentifier> iterator() {
        return delegate.iterator();
    }
}

