package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field to create a database index on the corresponding column.
 * Can be combined with {@code unique = true} to create a unique index.
 *
 * <p>Note: indexes are also created automatically for fields annotated with
 * {@code @Filter} and for foreign key columns.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Indexed {

    /**
     * Whether the index should enforce uniqueness. Default is false.
     *
     * @return true if the index should be unique, false otherwise
     */
    boolean unique() default false;
}
