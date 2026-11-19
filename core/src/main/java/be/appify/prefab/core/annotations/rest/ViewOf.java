package be.appify.prefab.core.annotations.rest;

import be.appify.prefab.core.annotations.Aggregate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a frontend view record to the aggregate whose generated service supplies its source rows.
 *
 * @see Aggregate
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ViewOf {
    /**
     * The aggregate that supplies this view's source data.
     *
     * @return the aggregate type
     */
    Class<?> value();
}
