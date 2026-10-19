package be.appify.prefab.core.util;

/**
 * Builds the PostgreSQL expression that reduces a column to the first letters of its words, for example "Spring Boot
 * guide" becomes "sbg". Shared by the generated index and the query executors so that the index stays usable by the
 * query.
 */
public final class InitialsExpression {
    /** The operator class that makes the index usable for {@code LIKE 'prefix%'} regardless of the collation. */
    public static final String OPERATOR_CLASS = "text_pattern_ops";

    private InitialsExpression() {
    }

    /**
     * Builds the expression.
     *
     * @param column
     *         the unquoted column name
     * @return the SQL expression
     */
    public static String of(String column) {
        return "regexp_replace(lower(\"%s\"), '[^[:alnum:]]*([[:alnum:]])[[:alnum:]]*', '\\1', 'g')".formatted(column);
    }

    /**
     * Builds the index column definition: the expression with the operator class.
     *
     * @param column
     *         the unquoted column name
     * @return the SQL index column
     */
    public static String indexColumn(String column) {
        return of(column) + " " + OPERATOR_CLASS;
    }
}
