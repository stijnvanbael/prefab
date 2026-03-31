package be.appify.prefab.postgres.spring.data.jdbc;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.relational.core.mapping.SqlIdentifierSanitizer;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.util.Assert;

/**
 * Evaluator for Spring Expression Language (SpEL) expressions that are expected to evaluate to SQL identifiers. It uses a provided
 * {@link EvaluationContextProvider} to create an evaluation context for evaluating the expression, and then sanitizes the result using a
 * {@link SqlIdentifierSanitizer} to ensure it is a valid SQL identifier. The resulting sanitized string is then wrapped in a
 * {@link SqlIdentifier} instance, which can be used in Spring Data JDBC mapping contexts.
 */
public class SqlIdentifierExpressionEvaluator {

    private EvaluationContextProvider provider;

    private SqlIdentifierSanitizer sanitizer = SqlIdentifierSanitizer.words();
    private Environment environment = new StandardEnvironment();

    /**
     * Constructs a new SqlIdentifierExpressionEvaluator with the given EvaluationContextProvider.
     *
     * @param provider
     *         the EvaluationContextProvider to use for creating evaluation contexts when evaluating expressions. Must not
     */
    public SqlIdentifierExpressionEvaluator(EvaluationContextProvider provider) {
        this.provider = provider;
    }

    /**
     * Evaluates the given ValueExpression and returns the result as a SqlIdentifier. The expression is evaluated using an EvaluationContext
     * provided by the EvaluationContextProvider, and the resulting value is sanitized to ensure it is a valid SQL identifier. If the
     * expression evaluates to null, an EvaluationException is thrown. If the expression evaluates to a non-string value, it is converted to
     * a string before sanitization. The isForceQuote parameter determines whether the resulting SqlIdentifier should be quoted or not.
     *
     * @param expression
     *         the ValueExpression to evaluate. Must not be null.
     * @param isForceQuote
     *         whether to force the resulting SqlIdentifier to be quoted, regardless of its content. If true, the resulting SqlIdentifier
     *         will be
     * @return the evaluated and sanitized SqlIdentifier
     * @throws EvaluationException
     *         if the expression evaluates to null or if there is an error during evaluation
     */
    public SqlIdentifier evaluate(ValueExpression expression, boolean isForceQuote) throws EvaluationException {
        Assert.notNull(expression, "Expression must not be null.");

        EvaluationContext evaluationContext = provider.getEvaluationContext(null);
        ValueEvaluationContext valueEvaluationContext = ValueEvaluationContext.of(environment, evaluationContext);

        Object value = expression.evaluate(valueEvaluationContext);
        if (value instanceof SqlIdentifier sqlIdentifier) {
            return sqlIdentifier;
        }

        if (value == null) {
            throw new EvaluationException("Expression '%s' evaluated to 'null'".formatted(expression));
        }

        String sanitizedResult = sanitizer.sanitize(value.toString());
        return isForceQuote ? SqlIdentifier.quoted(sanitizedResult) : SqlIdentifier.unquoted(sanitizedResult);
    }

    /**
     * Sets the EvaluationContextProvider to use for evaluating expressions. This allows changing the provider after the evaluator has been
     * constructed, which can be useful in certain scenarios such as testing or when the provider needs to be dynamically determined. The
     * provided provider must not be null, and an IllegalArgumentException will be thrown if a null provider is passed.
     *
     * @param provider
     *         the EvaluationContextProvider to use for evaluating expressions. Must not be null.
     */
    public void setProvider(EvaluationContextProvider provider) {
        Assert.notNull(provider, "EvaluationContextProvider must not be null");
        this.provider = provider;
    }

    /**
     * Sets the Environment to use for evaluating expressions. This allows changing the environment after the evaluator has been
     * constructed, which can be useful in certain scenarios such as testing or when the environment needs to be dynamically determined. The
     * provided environment must not be null, and an IllegalArgumentException will be thrown if a null environment is passed.
     *
     * @param environment
     *         he Environment to use for evaluating expressions. Must not be null.
     */
    public void setEnvironment(Environment environment) {
        Assert.notNull(environment, "Environment must not be null");
        this.environment = environment;
    }
}
