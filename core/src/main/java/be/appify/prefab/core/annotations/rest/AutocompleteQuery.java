package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime metadata on a generated {@code autocompleteBy<Field>} repository method.
 *
 * <p>{@link Autocomplete} only exists in source code, so the processor repeats the settings of the annotated field
 * with this annotation. Repository implementations that cannot execute the generated native query, such as the
 * in-memory test backend, read it to honour the declared scan mode and match strategy. Do not use it in application
 * code: it is generated.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutocompleteQuery {
    /**
     * The name of the aggregate property whose distinct values are returned.
     *
     * @return the property name.
     */
    String property();

    /**
     * Where in the value the query term must appear.
     *
     * @return the scan mode.
     */
    ScanMode scanMode();

    /**
     * How the query term is compared to the values.
     *
     * @return the match strategy.
     */
    MatchStrategy matchStrategy();
}
