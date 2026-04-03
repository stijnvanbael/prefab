package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specifies the previous name of a database table or column, used to generate a rename migration. */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface DbRename {
    /**
     * The previous name of the table or column (in Java naming style, will be converted to snake_case).
     *
     * @return The previous name.
     */
    String value();
}
