package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field on the aggregate root to expose an HTTP endpoint for field-level autocompletion.
 * Generates a dedicated endpoint returning distinct matching values for the annotated field.
 *
 * <p>Two orthogonal dimensions control how the query term is matched:
 * <ul>
 *   <li>{@link #scanMode()} — whether the term must appear at the start ({@link ScanMode#PREFIX})
 *       or anywhere ({@link ScanMode#CONTAINS}) in the value.</li>
 *   <li>{@link #matchStrategy()} — whether the comparison is exact, case-insensitive, or fuzzy.</li>
 * </ul>
 *
 * <p><strong>Migration from {@code ignoreCase}</strong>: the old boolean attribute has been removed.
 * Use {@code matchStrategy = MatchStrategy.IGNORE_CASE} as the equivalent, or
 * {@code matchStrategy = MatchStrategy.EXACT} for the former {@code ignoreCase = false} default.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Autocomplete {
    /**
     * The path to use for this endpoint, relative to the aggregate root base path.
     * Default is the kebab-case version of the field name with "/autocomplete" appended.
     *
     * @return The path to use for this endpoint.
     */
    String path() default "";

    /**
     * Where in the field value the query term must appear.
     * Defaults to {@link ScanMode#PREFIX} (values that start with the term).
     *
     * @return the scan mode to apply.
     */
    ScanMode scanMode() default ScanMode.PREFIX;

    /**
     * How the query term is compared to field values.
     * Defaults to {@link MatchStrategy#IGNORE_CASE}.
     *
     * @return the match strategy to apply.
     */
    MatchStrategy matchStrategy() default MatchStrategy.IGNORE_CASE;

    /**
     * Security settings for this endpoint. Default is secured with no required authorities.
     *
     * @return Security settings for this endpoint.
     */
    Security security() default @Security;
}
