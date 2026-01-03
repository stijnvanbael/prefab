package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specifies a default value for a database field. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DbDefaultValue {
    /**
     * The default value as a SQL expression.
     * @return The default value as a SQL expression.
     */
    String value();
}
